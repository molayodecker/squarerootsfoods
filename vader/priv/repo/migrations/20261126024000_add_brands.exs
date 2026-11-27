defmodule Vader.Repo.Migrations.AddBrands do
  use Ecto.Migration

  def change do
    create table(:brands) do
      add :slug, :string, null: false
      add :name, :string, null: false
      add :category, :string, null: false, default: "Snacks"
      add :image_url, :string, null: false
      timestamps(type: :utc_datetime)
    end

    create unique_index(:brands, [:slug])

    alter table(:goods) do
      add :brand_id, references(:brands, on_delete: :nilify_all)
      add :image_url, :string
      add :category, :string
    end
  end
end
