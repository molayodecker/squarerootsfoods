defmodule Vader.Paystack do
  alias Vader.Orders
  alias Vader.Orders.Order

  def secret, do: Application.get_env(:vader, :paystack_secret)
  def demo?, do: secret() in [nil, ""]

  def public_base do
    Application.get_env(:vader, :public_base_url, "http://127.0.0.1:8080")
    |> to_string()
    |> String.trim_trailing("/")
  end

  def initialize(%{} = params) do
    reference = blank(params["reference"])
    amount = int(params["amount_minor"])
    currency = params["currency"] || "GHS"
    email = blank(params["email"]) || "payments@example.com"
    machine = Vader.Machines.get_by_no(params["machine_no"])

    cond do
      reference == nil ->
        {:error, 400, "reference and amount_minor are required"}

      amount <= 0 ->
        {:error, 400, "reference and amount_minor are required"}

      true ->
        open_checkout(reference, amount, currency, email, machine, params["items"] || [])
    end
  end

  def verify(reference) when is_binary(reference) and reference != "" do
    case Orders.get_by_reference(reference) do
      nil ->
        {:error, 404, "Unknown payment"}

      %Order{payment_method: "cash"} ->
        {:error, 404, "Unknown payment"}

      %Order{demo: true} = order ->
        {:ok, %{status: order.status, demo: true, reference: reference, message: demo_message(order)}}

      order ->
        verify_live(order)
    end
  end

  def verify(_), do: {:error, 400, "reference is required"}

  def confirm_demo(reference) do
    case Orders.get_by_reference(reference) do
      %Order{demo: true, status: "pending"} = order ->
        Orders.mark_paid(order)
        :ok

      %Order{demo: true} ->
        :ok

      _ ->
        {:error, "Demo payment not found"}
    end
  end

  def refund(reference, amount, reason) do
    case Orders.get_by_reference(reference) do
      %Order{payment_method: "cash"} ->
        {:error, 409, "Record cash refunds through the operator desk"}

      %Order{demo: true} = order ->
        Orders.update_order(order, %{status: "refunded"})
        {:ok, %{demo: true, message: "Demo refund recorded. No card was charged."}}

      %Order{} = order ->
        refund_live(order, amount, reason)

      nil ->
        {:error, 404, "Unknown payment"}
    end
  end

  def apply_webhook(%{"event" => "charge.success", "data" => %{"reference" => reference}}) do
    case Orders.get_by_reference(reference) do
      %Order{payment_method: "paystack"} = order -> Orders.mark_paid(order)
      _ -> :ok
    end

    :ok
  end

  def apply_webhook(_), do: :ok

  defp open_checkout(reference, amount, currency, email, machine, items) do
    case Orders.get_by_reference(reference) do
      %Order{payment_method: "cash"} ->
        {:error, 409, "This reference cannot be used for Paystack"}

      %Order{} = existing ->
        {:ok, session(existing)}

      nil ->
        demo = demo?()

        with {:ok, order} <-
               Orders.create_order(%{
                 machine_id: machine && machine.id,
                 reference: reference,
                 email: email,
                 amount_minor: amount,
                 currency: currency,
                 status: "pending",
                 demo: demo,
                 items: normalize_items(items)
               }) do
          if demo do
            {:ok, session(order)}
          else
            start_live(order, machine)
          end
        end
    end
  end

  defp start_live(order, machine) do
    body = %{
      email: order.email,
      amount: order.amount_minor,
      currency: order.currency,
      reference: order.reference,
      callback_url: public_base() <> "/pay/return",
      metadata: %{machine_no: machine && machine.machine_no, items: order.items}
    }

    case request(:post, "/transaction/initialize", body) do
      {:ok, %{"data" => data}} ->
        url = data["authorization_url"] || ""
        {:ok, session(order, url)}

      {:error, message} ->
        {:error, 502, message}
    end
  end

  defp verify_live(order) do
    case request(:get, "/transaction/verify/#{URI.encode(order.reference)}", nil) do
      {:ok, %{"data" => data} = body} ->
        status = normalize_status(data["status"])
        if status == "success", do: Orders.mark_paid(order)

        message =
          if status == "failed",
            do: data["gateway_response"] || body["message"] || "The payment was declined.",
            else: ""

        {:ok, %{status: status, demo: false, reference: order.reference, message: message}}

      {:error, message} ->
        {:ok, %{status: "pending", demo: false, reference: order.reference, message: message}}
    end
  end

  defp refund_live(order, amount, reason) do
    payload = %{transaction: order.reference, amount: amount, merchant_note: reason || ""}

    case request(:post, "/refund", payload) do
      {:ok, body} ->
        Orders.update_order(order, %{status: "refunded"})
        {:ok, %{demo: false, message: body["message"] || "Refund submitted"}}

      {:error, message} ->
        {:error, 502, message}
    end
  end

  defp request(method, path, body) do
    opts = [auth: {:bearer, secret()}, receive_timeout: 30_000]
    opts = if body, do: Keyword.put(opts, :json, body), else: opts
    result = apply(Req, method, ["https://api.paystack.co" <> path, opts])

    case result do
      {:ok, %{status: status, body: resp}} when status in 200..299 ->
        {:ok, resp}

      {:ok, %{body: resp}} when is_map(resp) ->
        {:error, resp["message"] || "Paystack rejected the request"}

      {:ok, %{status: status}} ->
        {:error, "Paystack error #{status}"}

      {:error, reason} ->
        {:error, inspect(reason)}
    end
  end

  defp session(order, url \\ nil) do
    %{
      reference: order.reference,
      authorization_url: url || demo_url(order.reference),
      amount_minor: order.amount_minor,
      currency: order.currency,
      demo: order.demo
    }
  end

  defp demo_url(reference) do
    public_base() <> "/pay/demo?reference=" <> URI.encode_www_form(reference)
  end

  defp normalize_status("success"), do: "success"
  defp normalize_status("failed"), do: "failed"
  defp normalize_status("reversed"), do: "failed"
  # An initialized checkout stays "abandoned" until the customer finishes paying.
  defp normalize_status("abandoned"), do: "pending"
  defp normalize_status(_), do: "pending"

  defp demo_message(%{status: "success"}), do: "Demo payment"
  defp demo_message(%{status: "refunded"}), do: "Demo refund recorded"
  defp demo_message(_), do: "Waiting for the demo checkout"

  defp normalize_items(items) when is_list(items), do: Enum.filter(items, &is_map/1)
  defp normalize_items(_), do: []

  defp blank(value) when is_binary(value) do
    case String.trim(value) do
      "" -> nil
      other -> other
    end
  end

  defp blank(_), do: nil

  defp int(value) when is_integer(value), do: value
  defp int(value) when is_float(value), do: trunc(value)

  defp int(value) when is_binary(value) do
    case Integer.parse(value) do
      {number, _} -> number
      :error -> 0
    end
  end

  defp int(_), do: 0
end
