defmodule Vader.Settings.Cabinet do
  use Ecto.Schema
  import Ecto.Changeset

  schema "cabinet_settings" do
    field :cash_enabled, :boolean
    field :temperature_enabled, :boolean
    field :humidity_enabled, :boolean
    field :temperature_c, :float
    field :humidity_percent, :float
    field :climate_read_at, :utc_datetime
    timestamps(type: :utc_datetime)
  end

  def changeset(cabinet, attrs) do
    cabinet
    |> cast(attrs, [
      :cash_enabled,
      :temperature_enabled,
      :humidity_enabled,
      :temperature_c,
      :humidity_percent,
      :climate_read_at
    ])
  end
end
