defmodule MachineApiWeb.Admin.MachineController do
  use MachineApiWeb, :controller

  alias MachineApi.Machines

  def index(conn, _params) do
    json(conn, %{data: Machines.list_machines_with_counts()})
  end

  def create(conn, params) do
    case Machines.create_machine(params) do
      {:ok, machine} -> conn |> put_status(:created) |> json(%{data: Machines.json(machine)})
      {:error, changeset} -> invalid(conn, changeset)
    end
  end

  def update(conn, %{"id" => id} = params) do
    case Machines.update_machine(Machines.get_machine!(id), params) do
      {:ok, machine} -> json(conn, %{data: Machines.json(machine)})
      {:error, changeset} -> invalid(conn, changeset)
    end
  end

  defp invalid(conn, changeset) do
    conn |> put_status(:unprocessable_entity) |> json(%{message: "Could not save the machine", errors: errors_on(changeset)})
  end

  defp errors_on(changeset) do
    Ecto.Changeset.traverse_errors(changeset, fn {msg, opts} ->
      Enum.reduce(opts, msg, fn {key, value}, acc ->
        String.replace(acc, "%{#{key}}", to_string(value))
      end)
    end)
  end
end
