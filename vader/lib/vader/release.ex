defmodule Vader.Release do
  @moduledoc """
  One-off commands for a release. They run in their own process and then exit.
  """

  @app :vader

  def migrate do
    load_app()

    for repo <- repos() do
      {:ok, _, _} = Ecto.Migrator.with_repo(repo, &Ecto.Migrator.run(&1, :up, all: true))
    end
  end

  def rollback(repo, version) do
    load_app()
    {:ok, _, _} = Ecto.Migrator.with_repo(repo, &Ecto.Migrator.run(&1, :down, to: version))
  end

  def seed do
    migrate()
    {:ok, _} = Application.ensure_all_started(@app)
    Code.eval_file(Application.app_dir(@app, "priv/repo/seeds.exs"))
  end

  defp repos do
    Application.fetch_env!(@app, :ecto_repos)
  end

  defp load_app do
    Application.load(@app)
  end
end
