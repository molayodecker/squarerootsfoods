defmodule MachineApi.Commissioning.LaneCheck do
  use Ecto.Schema
  import Ecto.Changeset

  schema "lane_checks" do
    field :lane, :integer
    field :row, :integer
    field :coil, :integer
    field :result, :string
    field :detail, :string, default: ""
    belongs_to :report, MachineApi.Commissioning.Report
    timestamps(type: :utc_datetime)
  end

  def changeset(check, attrs) do
    check
    |> cast(attrs, [:report_id, :lane, :row, :coil, :result, :detail])
    |> validate_required([:report_id, :lane, :row, :coil, :result])
    |> validate_inclusion(:result, ["turned", "stuck", "empty", "untested"])
  end
end
