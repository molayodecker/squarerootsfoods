defmodule Vader.Fleet.Release do
  use Ecto.Schema
  import Ecto.Changeset

  schema "releases" do
    field :version, :string, default: ""
    field :version_code, :integer, default: 0
    field :url, :string, default: ""
    field :sha256, :string, default: ""
    field :required_version, :string, default: ""
    field :notes, :string, default: ""
    timestamps(type: :utc_datetime)
  end

  def changeset(release, attrs) do
    release
    |> cast(attrs, [:version, :version_code, :url, :sha256, :required_version, :notes])
    |> validate_required([:version, :version_code])
    |> validate_number(:version_code, greater_than_or_equal_to: 0)
  end
end
