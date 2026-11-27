defmodule VaderWeb.Admin.BrandController do
  use VaderWeb, :controller

  alias Vader.Brands

  def index(conn, _params) do
    json(conn, %{data: Enum.map(Brands.list_brands(), &Brands.json/1)})
  end
end
