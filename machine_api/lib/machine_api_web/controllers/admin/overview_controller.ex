defmodule MachineApiWeb.Admin.OverviewController do
  use MachineApiWeb, :controller

  alias MachineApi.{Machines, Orders, Paystack}

  def show(conn, _params) do
    machines = Machines.list_machines_with_counts()

    json(conn, %{
      data: %{
        machines: length(machines),
        lanes: Enum.sum(Enum.map(machines, & &1.lanes)),
        empty_lanes: Enum.sum(Enum.map(machines, & &1.empty_lanes)),
        paystack: paystack_mode(),
        sales: Orders.sales_totals()
      }
    })
  end

  defp paystack_mode do
    if Paystack.demo?(), do: "demo", else: "live"
  end
end
