defmodule MachineApi.Accounts do
  alias MachineApi.Accounts.Operator
  alias MachineApi.Repo

  @iterations 20_000

  def hash_password(password) when is_binary(password) do
    salt = :crypto.strong_rand_bytes(16)
    hash = :crypto.pbkdf2_hmac(:sha256, password, salt, @iterations, 32)
    Base.encode64(salt) <> "$" <> Base.encode64(hash)
  end

  def verify_password(password, stored) when is_binary(password) and is_binary(stored) do
    case String.split(stored, "$", parts: 2) do
      [salt_b64, hash_b64] ->
        with {:ok, salt} <- Base.decode64(salt_b64),
             {:ok, expected} <- Base.decode64(hash_b64) do
          actual = :crypto.pbkdf2_hmac(:sha256, password, salt, @iterations, 32)
          Plug.Crypto.secure_compare(actual, expected)
        else
          _ -> false
        end

      _ ->
        false
    end
  end

  def verify_password(_, _), do: false

  def get_operator(id), do: Repo.get(Operator, id)

  def get_by_email(email), do: Repo.get_by(Operator, email: email)

  def authenticate(email, password) do
    operator = get_by_email(email)

    cond do
      operator && verify_password(password, operator.password_hash) ->
        {:ok, operator}

      true ->
        hash_password("not-the-password")
        {:error, :invalid}
    end
  end

  def ensure_operator(email, password) do
    case get_by_email(email) do
      nil ->
        %Operator{}
        |> Operator.changeset(%{email: email, password: password})
        |> Repo.insert()

      operator ->
        {:ok, operator}
    end
  end

  def sign(operator) do
    Phoenix.Token.sign(MachineApiWeb.Endpoint, "operator", operator.id)
  end
end
