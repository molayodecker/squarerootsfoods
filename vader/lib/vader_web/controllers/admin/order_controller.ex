defmodule VaderWeb.Admin.OrderController do
  use VaderWeb, :controller

  alias Vader.Orders

  alias Vader.Paystack

  def index(conn, _params) do
    json(conn, %{data: Enum.map(Orders.list_orders(), &Orders.json/1)})
  end

  def refund(conn, %{"id" => reference}) do
    case Orders.get_by_reference(reference) do
      nil ->
        conn |> put_status(:not_found) |> json(%{message: "Unknown payment"})

      %{status: "success"} = order ->
        case Paystack.refund(order.reference, order.amount_minor, "operator") do
          {:ok, body} ->
            json(conn, %{
              data: Orders.json(Orders.get_by_reference(reference)),
              message: body.message
            })

          {:error, status, message} ->
            conn |> put_status(status) |> json(%{message: message})
        end

      _order ->
        conn |> put_status(:conflict) |> json(%{message: "Only a paid order can be refunded"})
    end
  end
end
