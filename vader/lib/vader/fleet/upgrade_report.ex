defmodule Vader.Fleet.UpgradeReport do
  use Ecto.Schema
  import Ecto.Changeset

  schema "upgrade_reports" do
    field :machine_no, :string, default: ""
    field :number, :string, default: ""
    field :before_version, :string, default: ""
    field :after_version, :string, default: ""
    field :state, :string, default: ""
    field :progress, :integer, default: 0
    field :error, :string, default: ""
    belongs_to :machine, Vader.Machines.Machine
    timestamps(type: :utc_datetime)
  end

  def changeset(report, attrs) do
    report
    |> cast(attrs, [
      :machine_id,
      :machine_no,
      :number,
      :before_version,
      :after_version,
      :state,
      :progress,
      :error
    ])
    |> validate_required([:machine_no, :state])
  end
end
