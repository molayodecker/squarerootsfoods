defmodule VaderWeb.Admin.CommissionController do
  use VaderWeb, :controller

  alias Vader.Commissioning

  def index(conn, _params) do
    json(conn, %{data: Enum.map(Commissioning.latest(), &Commissioning.json/1)})
  end
end
