defmodule Vader.Settings do
  @moduledoc """
  Operator choices saved from the desk.

  A saved choice wins, so the cabinet can change without a restart.
  Cash falls back to `CASH_ENABLED` until the desk saves it.
  Temperature and humidity stay off until the desk turns them on.
  """

  alias Vader.Repo
  alias Vader.Settings.Cabinet

  def cash_enabled? do
    case saved(:cash_enabled) do
      nil -> env_cash?()
      value -> value
    end
  end

  def temperature_enabled?, do: saved(:temperature_enabled) == true
  def humidity_enabled?, do: saved(:humidity_enabled) == true

  def status do
    cabinet = Repo.one(Cabinet)

    %{
      cash_enabled: cash_enabled?(),
      source: source(cabinet, :cash_enabled, "environment"),
      temperature_enabled: temperature_enabled?(),
      temperature_source: source(cabinet, :temperature_enabled, "default"),
      humidity_enabled: humidity_enabled?(),
      humidity_source: source(cabinet, :humidity_enabled, "default"),
      temperature_c: reading(cabinet, :temperature_c, :temperature_enabled),
      humidity_percent: reading(cabinet, :humidity_percent, :humidity_enabled),
      climate_read_at: cabinet && cabinet.climate_read_at
    }
  end

  def cash_status, do: Map.take(status(), [:cash_enabled, :source])

  def put(attrs) when is_map(attrs) do
    cabinet = Repo.one(Cabinet) || %Cabinet{}

    changes =
      attrs
      |> clear_reading(:temperature_enabled, :temperature_c)
      |> clear_reading(:humidity_enabled, :humidity_percent)

    cabinet
    |> Cabinet.changeset(changes)
    |> Repo.insert_or_update()
  end

  def put_cash(enabled) when is_boolean(enabled), do: put(%{cash_enabled: enabled})

  def record_climate(params) when is_map(params) do
    cabinet = Repo.one(Cabinet) || %Cabinet{}
    now = DateTime.utc_now() |> DateTime.truncate(:second)

    changes =
      %{}
      |> maybe_reading(cabinet, params, "temperature_c", :temperature_enabled, :temperature_c)
      |> maybe_reading(cabinet, params, "humidity_percent", :humidity_enabled, :humidity_percent)

    if changes == %{} do
      {:ok, cabinet}
    else
      cabinet
      |> Cabinet.changeset(Map.put(changes, :climate_read_at, now))
      |> Repo.insert_or_update()
    end
  end

  defp saved(field) do
    case Repo.one(Cabinet) do
      %Cabinet{} = cabinet -> Map.get(cabinet, field)
      _ -> nil
    end
  end

  defp source(nil, _field, fallback), do: fallback

  defp source(cabinet, field, fallback) do
    if is_boolean(Map.get(cabinet, field)), do: "desk", else: fallback
  end

  defp reading(nil, _value, _switch), do: nil

  defp reading(cabinet, value, switch) do
    if Map.get(cabinet, switch) == true, do: Map.get(cabinet, value), else: nil
  end

  defp clear_reading(changes, switch, reading) do
    case Map.fetch(changes, switch) do
      {:ok, false} -> Map.put(changes, reading, nil)
      _ -> changes
    end
  end

  defp maybe_reading(changes, cabinet, params, key, switch, field) do
    if Map.get(cabinet, switch) == true and Map.has_key?(params, key) do
      case number(params[key]) do
        nil -> changes
        value -> Map.put(changes, field, value)
      end
    else
      changes
    end
  end

  defp number(value) when is_float(value), do: value
  defp number(value) when is_integer(value), do: value * 1.0

  defp number(value) when is_binary(value) do
    case Float.parse(value) do
      {parsed, _} -> parsed
      :error -> nil
    end
  end

  defp number(_), do: nil

  defp env_cash?, do: Application.get_env(:vader, :cash_enabled, false) == true
end
