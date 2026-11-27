defmodule MachineApi.Commissioning.Report do
  use Ecto.Schema
  import Ecto.Changeset

  schema "commission_reports" do
    field :board, :string, default: ""
    field :serial_device, :string, default: ""
    field :baud, :integer, default: 9600
    field :rows, :integer, default: 0
    field :coils, :integer, default: 0
    field :simulate, :boolean, default: true
    field :payment_lane, :integer, default: 0
    field :payment_reference, :string, default: ""
    field :payment_status, :string, default: ""
    field :status, :string, default: "draft"
    belongs_to :machine, MachineApi.Machines.Machine
    has_many :checks, MachineApi.Commissioning.LaneCheck
    timestamps(type: :utc_datetime)
  end

  def changeset(report, attrs) do
    report
    |> cast(attrs, [
      :machine_id,
      :board,
      :serial_device,
      :baud,
      :rows,
      :coils,
      :simulate,
      :payment_lane,
      :payment_reference,
      :payment_status,
      :status
    ])
    |> validate_inclusion(:status, ["draft", "saved"])
    |> validate_required([:machine_id, :board, :rows, :coils])
    |> validate_inclusion(:board, ["vendele", "unknown"])
    |> validate_number(:rows, greater_than: 0, less_than_or_equal_to: 20)
    |> validate_number(:coils, greater_than: 0, less_than_or_equal_to: 20)
  end
end
