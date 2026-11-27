defmodule MachineApi.Machines.Brand do
  use Ecto.Schema
  import Ecto.Changeset

  schema "brands" do
    field :slug, :string
    field :name, :string
    field :category, :string, default: "Snacks"
    field :image_url, :string
    timestamps(type: :utc_datetime)
  end

  def changeset(brand, attrs) do
    brand
    |> cast(attrs, [:slug, :name, :category, :image_url])
    |> validate_required([:slug, :name, :category, :image_url])
    |> update_change(:name, &String.trim/1)
    |> unique_constraint(:slug)
  end
end
