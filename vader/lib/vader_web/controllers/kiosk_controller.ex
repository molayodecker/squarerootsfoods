defmodule VaderWeb.KioskController do
  use VaderWeb, :controller

  alias Vader.{Commissioning, Fleet, Machines, Orders, Paystack, Settings}

  def health(conn, _params) do
    json(conn, %{
      ok: true,
      paystack: not Paystack.demo?(),
      cash: Vader.cash_enabled?(),
      temperature: Settings.temperature_enabled?(),
      humidity: Settings.humidity_enabled?()
    })
  end

  def climate(conn, params) do
    Settings.record_climate(params)
    json(conn, %{ok: true, data: Settings.status()})
  end

  def goods(conn, params) do
    case Machines.list_goods_by_no(params["machine_no"]) do
      {:ok, goods} ->
        json(conn, %{code: 0, data: Enum.map(goods, &Machines.json/1)})

      {:error, :unknown_machine} ->
        error(conn, 404, "Unknown machine")
    end
  end

  def initialize(conn, params) do
    case Paystack.initialize(params) do
      {:ok, session} -> json(conn, session)
      {:error, status, message} -> error(conn, status, message)
      {:error, changeset} -> error(conn, 422, inspect(changeset.errors))
    end
  end

  def verify(conn, params) do
    case Paystack.verify(params["reference"]) do
      {:ok, body} -> json(conn, body)
      {:error, status, message} -> error(conn, status, message)
    end
  end

  def refund(conn, params) do
    amount = params["amount_minor"] || 0

    case Paystack.refund(params["reference"], amount, params["reason"]) do
      {:ok, body} -> json(conn, body)
      {:error, status, message} -> error(conn, status, message)
    end
  end

  def webhook(conn, params) do
    Paystack.apply_webhook(params)
    json(conn, %{ok: true})
  end

  def cash(conn, params) do
    if Vader.cash_enabled?() do
      case Orders.take_cash(params) do
        {:ok, body} -> json(conn, body)
        {:error, status, message} -> error(conn, status, message)
      end
    else
      error(conn, 404, "Cash is not enabled")
    end
  end

  def deliver(conn, params) do
    machine = Machines.get_by_no(params["machine_no"])
    order = Orders.get_by_reference(params["reference"] || "")
    lane = int(params["lane"])
    qty = int(params["qty"])
    ok = params["ok"] in [true, "true", 1]

    Orders.record_delivery(%{
      order_id: order && order.id,
      machine_id: machine && machine.id,
      reference: params["reference"] || "",
      lane: lane,
      qty: qty,
      ok: ok,
      detail: params["detail"] || ""
    })

    if ok && machine, do: Machines.take_stock(machine.id, lane, qty)
    json(conn, %{code: 0})
  end

  def commission(conn, params) do
    case Commissioning.record(params) do
      {:ok, report} -> json(conn, %{code: 0, data: Commissioning.json(report)})
      {:error, %Ecto.Changeset{}} -> error(conn, 422, "Could not save the install")
      {:error, message} when is_binary(message) -> error(conn, 400, message)
    end
  end

  def latest(conn, _params) do
    json(conn, Fleet.payload(Fleet.current_release()))
  end

  def upgrade(conn, params) do
    machine = Machines.get_by_no(params["machine_no"])

    Fleet.record_upgrade(%{
      machine_id: machine && machine.id,
      machine_no: params["machine_no"] || "",
      number: params["number"] || "",
      before_version: params["before_version"] || "",
      after_version: params["after_version"] || "",
      state: params["state"] || "",
      progress: int(params["progress"]),
      error: params["error"] || ""
    })

    json(conn, %{code: 0})
  end

  def demo(conn, params) do
    reference = params["reference"] || ""
    safe = Jason.encode!(reference)

    html = """
    <!doctype html>
    <html><head><meta name="viewport" content="width=device-width,initial-scale=1"><title>Demo checkout</title></head>
    <body style="font-family:sans-serif;padding:24px">
    <h1>Demo checkout</h1>
    <p>Reference #{Plug.HTML.html_escape(reference)}</p>
    <button id="pay" style="font-size:18px;padding:12px 20px">Mark as paid</button>
    <p id="out"></p>
    <script>
    document.getElementById("pay").onclick = async () => {
      const res = await fetch("/pay/demo/confirm", {
        method: "POST",
        headers: {"Content-Type": "application/json"},
        body: JSON.stringify({reference: #{safe}})
      });
      document.getElementById("out").textContent = res.ok ? "Paid. Look at the machine." : "Could not confirm.";
    };
    </script></body></html>
    """

    conn |> put_resp_content_type("text/html") |> send_resp(200, html)
  end

  def confirm_demo(conn, params) do
    case Paystack.confirm_demo(params["reference"]) do
      :ok -> json(conn, %{status: "success", reference: params["reference"]})
      {:error, message} -> error(conn, 404, message)
    end
  end

  def pay_return(conn, _params) do
    html = """
    <!doctype html><html><body style="font-family:sans-serif;padding:24px">
    <h1>Payment received</h1><p>You can look back at the machine.</p>
    </body></html>
    """

    conn |> put_resp_content_type("text/html") |> send_resp(200, html)
  end

  defp error(conn, status, message) do
    conn |> put_status(status) |> json(%{message: message})
  end

  defp int(value) when is_integer(value), do: value
  defp int(value) when is_binary(value) do
    case Integer.parse(value) do
      {number, _} -> number
      :error -> 0
    end
  end

  defp int(_), do: 0
end
