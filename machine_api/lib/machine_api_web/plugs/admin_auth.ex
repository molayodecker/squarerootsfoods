defmodule MachineApiWeb.Plugs.AdminAuth do
  import Plug.Conn
  import Phoenix.Controller

  alias MachineApi.Accounts

  def init(opts), do: opts

  def call(conn, _opts) do
    with ["Bearer " <> token] <- get_req_header(conn, "authorization"),
         {:ok, id} <- Phoenix.Token.verify(MachineApiWeb.Endpoint, "operator", token, max_age: 1_209_600),
         %{} = operator <- Accounts.get_operator(id) do
      assign(conn, :operator, operator)
    else
      _ ->
        conn
        |> put_status(:unauthorized)
        |> json(%{message: "Sign in required"})
        |> halt()
    end
  end
end
