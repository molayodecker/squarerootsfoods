defmodule Vader.Orders do
  import Ecto.Query
  alias Vader.Orders.{Delivery, Order}
  alias Vader.Repo

  def list_orders(limit \\ 100) do
    Repo.all(
      from o in Order,
        order_by: [desc: o.inserted_at],
        limit: ^limit,
        preload: [:machine, :deliveries]
    )
  end

  def get_by_reference(reference) do
    Order
    |> Repo.get_by(reference: reference)
    |> Repo.preload([:machine, :deliveries])
  end

  def sales_totals do
    today = DateTime.new!(Date.utc_today(), ~T[00:00:00], "Etc/UTC")
    month = DateTime.new!(Date.beginning_of_month(Date.utc_today()), ~T[00:00:00], "Etc/UTC")

    from(o in Order,
      where: o.status == "success",
      group_by: o.currency,
      select: %{
        currency: o.currency,
        total_minor: coalesce(sum(o.amount_minor), 0),
        today_minor:
          coalesce(
            sum(fragment("CASE WHEN ? >= ? THEN ? ELSE 0 END", o.inserted_at, ^today, o.amount_minor)),
            0
          ),
        month_minor:
          coalesce(
            sum(fragment("CASE WHEN ? >= ? THEN ? ELSE 0 END", o.inserted_at, ^month, o.amount_minor)),
            0
          )
      }
    )
    |> Repo.all()
    |> Enum.map(fn row ->
      %{
        currency: row.currency,
        total_minor: to_int(row.total_minor),
        today_minor: to_int(row.today_minor),
        month_minor: to_int(row.month_minor)
      }
    end)
  end

  def create_order(attrs), do: %Order{} |> Order.changeset(attrs) |> Repo.insert()

  def update_order(%Order{} = order, attrs), do: order |> Order.changeset(attrs) |> Repo.update()

  def record_delivery(attrs) do
    %Delivery{} |> Delivery.changeset(attrs) |> Repo.insert()
  end

  def mark_paid(%Order{} = order) do
    update_order(order, %{status: "success"})
  end

  def take_cash(params) when is_map(params) do
    reference = text(params["reference"])
    amount = int(params["amount_minor"])
    currency = params["currency"] || "GHS"
    machine = Vader.Machines.get_by_no(params["machine_no"] || "")

    cond do
      reference == nil or amount <= 0 ->
        {:error, 400, "reference and amount_minor are required"}

      true ->
        case get_by_reference(reference) ||
               create_paid_cash(reference, amount, currency, machine, params["items"] || []) do
          %Order{} = order ->
            {:ok, %{reference: order.reference, amount_minor: order.amount_minor, currency: order.currency, status: order.status}}

          {:ok, %Order{} = order} ->
            {:ok, %{reference: order.reference, amount_minor: order.amount_minor, currency: order.currency, status: order.status}}

          {:error, _changeset} ->
            {:error, 422, "Could not record the cash sale"}
        end
    end
  end

  defp create_paid_cash(reference, amount, currency, machine, items) do
    create_order(%{
      machine_id: machine && machine.id,
      reference: reference,
      email: "cash@machine",
      amount_minor: amount,
      currency: currency,
      status: "success",
      demo: false,
      items: if(is_list(items), do: Enum.filter(items, &is_map/1), else: [])
    })
  end

  defp text(value) when is_binary(value) do
    case String.trim(value) do
      "" -> nil
      other -> other
    end
  end

  defp text(_), do: nil

  defp int(value) when is_integer(value), do: value
  defp int(value) when is_float(value), do: trunc(value)

  defp int(value) when is_binary(value) do
    case Integer.parse(value) do
      {number, _} -> number
      :error -> 0
    end
  end

  defp int(_), do: 0

  def json(%Order{} = order) do
    %{
      id: order.id,
      reference: order.reference,
      email: order.email,
      amount_minor: order.amount_minor,
      currency: order.currency,
      status: order.status,
      demo: order.demo,
      machine_no: machine_no(order),
      items: order.items,
      deliveries: delivery_json(order.deliveries),
      inserted_at: order.inserted_at
    }
  end

  defp delivery_json(deliveries) when is_list(deliveries) do
    Enum.map(deliveries, fn delivery ->
      %{lane: delivery.lane, qty: delivery.qty, ok: delivery.ok, detail: delivery.detail}
    end)
  end

  defp delivery_json(_), do: []

  defp machine_no(%{machine: %{machine_no: no}}), do: no
  defp machine_no(_), do: nil

  defp to_int(nil), do: 0
  defp to_int(%Decimal{} = value), do: Decimal.to_integer(value)
  defp to_int(value) when is_integer(value), do: value
end
