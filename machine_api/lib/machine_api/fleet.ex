defmodule MachineApi.Fleet do
  alias MachineApi.Fleet.{Release, UpgradeReport}
  alias MachineApi.Repo

  def current_release do
    case Repo.one(Release) do
      nil ->
        {:ok, release} =
          %Release{}
          |> Release.changeset(%{version: "3.2.0", version_code: 320, notes: "No APK published yet."})
          |> Repo.insert()

        release

      release ->
        release
    end
  end

  def update_release(attrs) do
    current_release() |> Release.changeset(attrs) |> Repo.update()
  end

  def payload(%Release{} = release) do
    ready = release.url not in [nil, ""] and release.sha256 not in [nil, ""] and release.version_code > 0

    %{
      update: ready,
      version: release.version,
      version_code: release.version_code,
      url: release.url || "",
      sha256: release.sha256 || "",
      required_version: release.required_version || "",
      notes: release.notes || ""
    }
  end

  def record_upgrade(attrs) do
    %UpgradeReport{} |> UpgradeReport.changeset(attrs) |> Repo.insert()
  end
end
