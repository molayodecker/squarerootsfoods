defmodule VaderWeb.Admin.GoodController do
  use VaderWeb, :controller

  alias Vader.Machines

  def index(conn, %{"machine_id" => machine_id}) when machine_id not in [nil, ""] do
    goods = machine_id |> Machines.list_goods() |> Enum.map(&Machines.json/1)
    json(conn, %{data: goods})
  end

  def index(conn, _params) do
    json(conn, %{data: Enum.map(Machines.list_all_goods(), &Machines.json/1)})
  end

  def create(conn, params) do
    case Machines.create_good(params) do
      {:ok, good} -> conn |> put_status(:created) |> json(%{data: Machines.json(good)})
      {:error, changeset} -> invalid(conn, changeset)
    end
  end

  def update(conn, %{"id" => id} = params) do
    case Machines.update_good(Machines.get_good!(id), params) do
      {:ok, good} -> json(conn, %{data: Machines.json(good)})
      {:error, changeset} -> invalid(conn, changeset)
    end
  end

  def delete(conn, %{"id" => id}) do
    {:ok, _} = Machines.delete_good(Machines.get_good!(id))
    json(conn, %{ok: true})
  end

  defp invalid(conn, changeset) do
    conn
    |> put_status(:unprocessable_entity)
    |> json(%{message: "Could not save the product", errors: Ecto.Changeset.traverse_errors(changeset, fn {msg, _} -> msg end)})
  end
end
