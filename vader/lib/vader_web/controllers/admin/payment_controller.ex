defmodule VaderWeb.Admin.PaymentController do
  use VaderWeb, :controller

  alias Vader.Paystack

  def show(conn, _params) do
    json(conn, %{
      data: %{
        provider: "paystack",
        mode: if(Paystack.demo?(), do: "demo", else: "live"),
        public_base_url: Paystack.public_base()
      }
    })
  end
end
