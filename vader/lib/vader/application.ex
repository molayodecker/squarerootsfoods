defmodule Vader.Application do
  # See https://elixir.hexdocs.pm/Application.html
  # for more information on OTP Applications
  @moduledoc false

  use Application

  @impl true
  def start(_type, _args) do
    children = [
      VaderWeb.Telemetry,
      Vader.Repo,
      {DNSCluster, query: Application.get_env(:vader, :dns_cluster_query) || :ignore},
      {Phoenix.PubSub, name: Vader.PubSub},
      # Start a worker by calling: Vader.Worker.start_link(arg)
      # {Vader.Worker, arg},
      # Start to serve requests, typically the last entry
      VaderWeb.Endpoint
    ]

    # See https://elixir.hexdocs.pm/Supervisor.html
    # for other strategies and supported options
    opts = [strategy: :one_for_one, name: Vader.Supervisor]
    Supervisor.start_link(children, opts)
  end

  # Tell Phoenix to update the endpoint configuration
  # whenever the application is updated.
  @impl true
  def config_change(changed, _new, removed) do
    VaderWeb.Endpoint.config_change(changed, removed)
    :ok
  end
end
