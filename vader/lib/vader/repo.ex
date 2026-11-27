defmodule Vader.Repo do
  use Ecto.Repo,
    otp_app: :vader,
    adapter: Ecto.Adapters.Postgres
end
