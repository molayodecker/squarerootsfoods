defmodule MachineApi.Repo.Migrations.CreateCommissionReports do
  use Ecto.Migration

  def change do
    create table(:commission_reports) do
      add :machine_id, references(:machines, on_delete: :delete_all), null: false
      add :board, :string, null: false, default: ""
      add :serial_device, :string, null: false, default: ""
      add :baud, :integer, null: false, default: 9600
      add :rows, :integer, null: false, default: 0
      add :coils, :integer, null: false, default: 0
      add :simulate, :boolean, null: false, default: true
      add :payment_lane, :integer, null: false, default: 0
      add :payment_reference, :string, null: false, default: ""
      add :payment_status, :string, null: false, default: ""
      timestamps(type: :utc_datetime)
    end

    create index(:commission_reports, [:machine_id])

    create table(:lane_checks) do
      add :report_id, references(:commission_reports, on_delete: :delete_all), null: false
      add :lane, :integer, null: false
      add :row, :integer, null: false
      add :coil, :integer, null: false
      add :result, :string, null: false
      add :detail, :text, null: false, default: ""
      timestamps(type: :utc_datetime)
    end

    create index(:lane_checks, [:report_id])
  end
end
