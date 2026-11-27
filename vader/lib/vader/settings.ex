defmodule Vader.Settings do
  @moduledoc """
  Operator choices saved from the desk.

  A saved choice wins, so the cabinet can change without a restart.
  Cash falls back to `CASH_ENABLED` until the desk saves it.
  Temperature and humidity stay off until the desk turns them on.
  """

  import Ecto.Query

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
    cabinet = Repo.get!(Cabinet, 1)

    %{
      cash_enabled: if(is_nil(cabinet.cash_enabled), do: env_cash?(), else: cabinet.cash_enabled),
      source: source(cabinet, :cash_enabled, "environment"),
      temperature_enabled: cabinet.temperature_enabled == true,
      temperature_source: source(cabinet, :temperature_enabled, "default"),
      humidity_enabled: cabinet.humidity_enabled == true,
      humidity_source: source(cabinet, :humidity_enabled, "default"),
      temperature_c: reading(cabinet, :temperature_c, :temperature_enabled),
      humidity_percent: reading(cabinet, :humidity_percent, :humidity_enabled),
      climate_read_at: cabinet.climate_read_at
    }
  end

  def cash_status, do: Map.take(status(), [:cash_enabled, :source])

  def put(attrs) when is_map(attrs) do
    changes =
      attrs
      |> clear_reading(:temperature_enabled, :temperature_c)
      |> clear_reading(:humidity_enabled, :humidity_percent)

    update_cabinet(fn cabinet ->
      cabinet |> Cabinet.changeset(changes) |> Repo.update()
    end)
  end

  def put_cash(enabled) when is_boolean(enabled), do: put(%{cash_enabled: enabled})

  def record_climate(params) when is_map(params) do
    update_cabinet(fn cabinet ->
      changes =
        %{}
        |> maybe_reading(cabinet, params, "temperature_c", :temperature_enabled, :temperature_c)
        |> maybe_reading(cabinet, params, "humidity_percent", :humidity_enabled, :humidity_percent)

      if changes == %{} do
        {:ok, cabinet}
      else
        now = DateTime.utc_now() |> DateTime.truncate(:second)
        cabinet |> Cabinet.changeset(Map.put(changes, :climate_read_at, now)) |> Repo.update()
      end
    end)
  end

  defp update_cabinet(fun) do
    Repo.transaction(fn ->
      cabinet = Repo.one!(from c in Cabinet, where: c.id == 1, lock: "FOR UPDATE")

      case fun.(cabinet) do
        {:ok, updated} -> updated
        {:error, changeset} -> Repo.rollback(changeset)
      end
    end)
  end

  defp saved(field), do: Cabinet |> Repo.get!(1) |> Map.fetch!(field)

  defp source(cabinet, field, fallback) do
    if is_boolean(Map.get(cabinet, field)), do: "desk", else: fallback
  end

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
