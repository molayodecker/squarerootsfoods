defmodule Vader.Repo.Migrations.CreateCabinetSettings do
  use Ecto.Migration

  def change do
    create table(:cabinet_settings) do
      add :cash_enabled, :boolean
      timestamps(type: :utc_datetime)
    end
  end
end
