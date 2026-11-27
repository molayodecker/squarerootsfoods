defmodule MachineApi.Orders.Order do
  use Ecto.Schema
  import Ecto.Changeset

  schema "orders" do
    field :reference, :string
    field :email, :string
    field :amount_minor, :integer
    field :currency, :string
    field :status, :string, default: "pending"
    field :demo, :boolean, default: false
    field :items, {:array, :map}, default: []
    belongs_to :machine, MachineApi.Machines.Machine
    has_many :deliveries, MachineApi.Orders.Delivery
    timestamps(type: :utc_datetime)
  end

  def changeset(order, attrs) do
    order
    |> cast(attrs, [:machine_id, :reference, :email, :amount_minor, :currency, :status, :demo, :items])
    |> validate_required([:reference, :email, :amount_minor, :currency, :status])
    |> unique_constraint(:reference)
  end
end
