defmodule VaderWeb.Admin.SessionController do
  use VaderWeb, :controller

  alias Vader.Accounts

  def create(conn, %{"email" => email, "password" => password}) do
    case Accounts.authenticate(email, password) do
      {:ok, operator} ->
        json(conn, %{token: Accounts.sign(operator), email: operator.email})

      {:error, :invalid} ->
        conn |> put_status(:unauthorized) |> json(%{message: "Email or password is wrong"})
    end
  end

  def create(conn, _params) do
    conn |> put_status(:bad_request) |> json(%{message: "Email and password are required"})
  end
end
