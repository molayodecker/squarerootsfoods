defmodule Vader.Accounts.Operator do
  use Ecto.Schema
  import Ecto.Changeset

  schema "operators" do
    field :email, :string
    field :password, :string, virtual: true, redact: true
    field :password_hash, :string, redact: true
    timestamps(type: :utc_datetime)
  end

  def changeset(operator, attrs) do
    operator
    |> cast(attrs, [:email, :password])
    |> validate_required([:email, :password])
    |> validate_length(:password, min: 8)
    |> unique_constraint(:email)
    |> put_hash()
  end

  defp put_hash(changeset) do
    case get_change(changeset, :password) do
      nil -> changeset
      password -> put_change(changeset, :password_hash, Vader.Accounts.hash_password(password))
    end
  end
end
