defmodule MachineApi.Machines.Good do
  use Ecto.Schema
  import Ecto.Changeset

  schema "goods" do
    field :sku, :string
    field :name, :string
    field :price_minor, :integer
    field :lane, :integer
    field :stock, :integer, default: 0
    field :currency, :string, default: "GHS"
    field :image_url, :string
    field :category, :string
    belongs_to :brand, MachineApi.Machines.Brand
    belongs_to :machine, MachineApi.Machines.Machine
    timestamps(type: :utc_datetime)
  end

  def changeset(good, attrs) do
    good
    |> cast(attrs, [:machine_id, :sku, :name, :price_minor, :lane, :stock, :currency, :image_url, :category, :brand_id])
    |> validate_required([:machine_id, :sku, :name, :price_minor, :lane, :stock, :currency])
    |> validate_number(:price_minor, greater_than: 0)
    |> validate_number(:lane, greater_than: 0)
    |> validate_number(:stock, greater_than_or_equal_to: 0)
    |> update_change(:currency, &String.upcase/1)
    |> unique_constraint([:machine_id, :sku])
    |> unique_constraint([:machine_id, :lane])
  end
end
