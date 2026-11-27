defmodule VaderWeb.Admin.ReleaseController do
  use VaderWeb, :controller

  alias Vader.Fleet

  def show(conn, _params) do
    json(conn, %{data: Fleet.payload(Fleet.current_release())})
  end

  def update(conn, params) do
    attrs = %{
      version: params["version"] || "",
      version_code: params["version_code"] || 0,
      url: params["url"] || "",
      sha256: params["sha256"] || "",
      required_version: params["required_version"] || "",
      notes: params["notes"] || ""
    }

    case Fleet.update_release(attrs) do
      {:ok, release} -> json(conn, %{data: Fleet.payload(release)})
      {:error, changeset} ->
        conn
        |> put_status(:unprocessable_entity)
        |> json(%{message: "Could not save the release", errors: changeset.errors})
    end
  end
end
