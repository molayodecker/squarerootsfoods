defmodule MachineApiWeb.Admin.PaymentController do
  use MachineApiWeb, :controller

  alias MachineApi.Paystack

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
