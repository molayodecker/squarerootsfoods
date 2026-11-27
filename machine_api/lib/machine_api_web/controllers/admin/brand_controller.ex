defmodule MachineApiWeb.Admin.BrandController do
  use MachineApiWeb, :controller

  alias MachineApi.Brands

  def index(conn, _params) do
    json(conn, %{data: Enum.map(Brands.list_brands(), &Brands.json/1)})
  end
end
