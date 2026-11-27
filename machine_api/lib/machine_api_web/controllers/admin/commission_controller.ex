defmodule MachineApiWeb.Admin.CommissionController do
  use MachineApiWeb, :controller

  alias MachineApi.Commissioning

  def index(conn, _params) do
    json(conn, %{data: Enum.map(Commissioning.latest(), &Commissioning.json/1)})
  end
end
