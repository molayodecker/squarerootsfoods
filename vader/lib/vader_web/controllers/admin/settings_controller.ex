defmodule VaderWeb.Admin.SettingsController do
  use VaderWeb, :controller

  alias Vader.Settings

  def show(conn, _params) do
    json(conn, %{data: Settings.status()})
  end

  def update(conn, params) do
    attrs =
      %{}
      |> take_bool(params, "cash_enabled", :cash_enabled)
      |> take_bool(params, "temperature_enabled", :temperature_enabled)
      |> take_bool(params, "humidity_enabled", :humidity_enabled)

    if attrs == %{} do
      conn
      |> put_status(:unprocessable_entity)
      |> json(%{message: "Send cash_enabled, temperature_enabled, or humidity_enabled as true or false"})
    else
      case Settings.put(attrs) do
        {:ok, _} ->
          json(conn, %{data: Settings.status()})

        {:error, _changeset} ->
          conn
          |> put_status(:unprocessable_entity)
          |> json(%{message: "Could not save the setting"})
      end
    end
  end

  defp take_bool(attrs, params, key, field) do
    case Map.fetch(params, key) do
      {:ok, value} when is_boolean(value) -> Map.put(attrs, field, value)
      _ -> attrs
    end
  end
end
