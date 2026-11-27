defmodule MachineApi.Repo do
  use Ecto.Repo,
    otp_app: :machine_api,
    adapter: Ecto.Adapters.Postgres
end
