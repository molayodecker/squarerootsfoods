defmodule Vader.Machines do
  import Ecto.Query
  alias Vader.Brands
  alias Vader.Machines.{Good, Machine}
  alias Vader.Repo

  def list_machines do
    Repo.all(from m in Machine, order_by: [asc: m.machine_no])
  end

  def list_machines_with_counts do
    counts =
      Repo.all(
        from g in Good,
          group_by: g.machine_id,
          select:
            {g.machine_id, count(g.id),
             count(fragment("CASE WHEN ? <= 0 THEN 1 END", g.stock))}
      )
      |> Map.new(fn {id, lanes, empty} -> {id, {lanes, empty}} end)

    Enum.map(list_machines(), fn machine ->
      {lanes, empty} = Map.get(counts, machine.id, {0, 0})
      Map.merge(json(machine), %{lanes: lanes, empty_lanes: empty})
    end)
  end

  def get_machine!(id), do: Repo.get!(Machine, id)

  def get_by_no(machine_no) when is_binary(machine_no) do
    Repo.get_by(Machine, machine_no: machine_no)
  end

  def get_by_no(_), do: nil

  def create_machine(attrs), do: %Machine{} |> Machine.changeset(attrs) |> Repo.insert()
  def update_machine(%Machine{} = machine, attrs), do: machine |> Machine.changeset(attrs) |> Repo.update()

  def list_goods(machine_id) do
    Repo.all(from g in Good, where: g.machine_id == ^machine_id, order_by: [asc: g.lane])
  end

  def list_all_goods do
    Repo.all(from g in Good, order_by: [asc: g.machine_id, asc: g.lane], preload: [:machine])
  end

  def list_goods_by_no(machine_no) do
    case get_by_no(machine_no) do
      nil -> {:error, :unknown_machine}
      machine -> {:ok, list_goods(machine.id)}
    end
  end

  def create_good(attrs), do: %Good{} |> Good.changeset(Brands.apply_to_good(attrs)) |> Repo.insert()

  def get_good!(id), do: Repo.get!(Good, id)

  def update_good(%Good{} = good, attrs), do: good |> Good.changeset(attrs) |> Repo.update()

  def delete_good(%Good{} = good), do: Repo.delete(good)

  def take_stock(machine_id, lane, qty) when qty > 0 do
    case Repo.one(from g in Good, where: g.machine_id == ^machine_id and g.lane == ^lane) do
      nil ->
        :ok

      good ->
        good
        |> Good.changeset(%{stock: max(good.stock - qty, 0)})
        |> Repo.update()
    end
  end

  def take_stock(_, _, _), do: :ok

  def json(%Machine{} = machine) do
    %{
      id: machine.id,
      machine_no: machine.machine_no,
      name: machine.name,
      currency: machine.currency
    }
  end

  def json(%Good{} = good) do
    %{
      id: good.sku,
      record_id: good.id,
      machine_id: good.machine_id,
      name: good.name,
      price_minor: good.price_minor,
      lane: good.lane,
      stock: good.stock,
      currency: good.currency,
      image_url: good.image_url || "",
      category: good.category || ""
    }
    |> put_machine(good.machine)
  end

  defp put_machine(map, %Machine{machine_no: machine_no, name: name}) do
    Map.merge(map, %{machine_no: machine_no, machine_name: name})
  end

  defp put_machine(map, _), do: map
end
