defmodule Vader.Repo.Migrations.AddClimateSwitches do
  use Ecto.Migration

  def change do
    alter table(:cabinet_settings) do
      add :temperature_enabled, :boolean
      add :humidity_enabled, :boolean
      add :temperature_c, :float
      add :humidity_percent, :float
      add :climate_read_at, :utc_datetime
    end
  end
end
