defmodule MachineApi.Machines.Machine do
  use Ecto.Schema
  import Ecto.Changeset

  schema "machines" do
    field :machine_no, :string
    field :name, :string
    field :currency, :string, default: "GHS"
    has_many :goods, MachineApi.Machines.Good
    timestamps(type: :utc_datetime)
  end

  def changeset(machine, attrs) do
    machine
    |> cast(attrs, [:machine_no, :name, :currency])
    |> validate_required([:machine_no, :name, :currency])
    |> update_change(:machine_no, &String.trim/1)
    |> update_change(:currency, &String.upcase/1)
    |> unique_constraint(:machine_no)
  end
end
