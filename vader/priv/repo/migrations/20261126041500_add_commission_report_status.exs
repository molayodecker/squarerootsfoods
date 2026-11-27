defmodule Vader.Repo.Migrations.AddCommissionReportStatus do
  use Ecto.Migration

  def change do
    alter table(:commission_reports) do
      add :status, :string, null: false, default: "saved"
    end
  end
end