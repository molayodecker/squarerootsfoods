defmodule Vader.Orders.Delivery do
  use Ecto.Schema
  import Ecto.Changeset

  schema "deliveries" do
    field :reference, :string
    field :lane, :integer
    field :qty, :integer
    field :ok, :boolean
    field :detail, :string, default: ""
    belongs_to :order, Vader.Orders.Order
    belongs_to :machine, Vader.Machines.Machine
    timestamps(type: :utc_datetime)
  end

  def changeset(delivery, attrs) do
    delivery
    |> cast(attrs, [:order_id, :machine_id, :reference, :lane, :qty, :ok, :detail])
    |> validate_required([:reference, :lane, :qty, :ok])
  end
end
