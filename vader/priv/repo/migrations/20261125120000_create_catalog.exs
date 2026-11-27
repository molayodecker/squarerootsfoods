defmodule Vader.Repo.Migrations.CreateCatalog do
  use Ecto.Migration

  def change do
    create table(:machines) do
      add :machine_no, :string, null: false
      add :name, :string, null: false
      add :currency, :string, null: false, default: "NGN"
      timestamps(type: :utc_datetime)
    end

    create unique_index(:machines, [:machine_no])

    create table(:goods) do
      add :machine_id, references(:machines, on_delete: :delete_all), null: false
      add :sku, :string, null: false
      add :name, :string, null: false
      add :price_minor, :integer, null: false
      add :lane, :integer, null: false
      add :stock, :integer, null: false, default: 0
      add :currency, :string, null: false, default: "NGN"
      timestamps(type: :utc_datetime)
    end

    create unique_index(:goods, [:machine_id, :sku])
    create unique_index(:goods, [:machine_id, :lane])

    create table(:orders) do
      add :machine_id, references(:machines, on_delete: :nilify_all)
      add :reference, :string, null: false
      add :email, :string, null: false
      add :amount_minor, :integer, null: false
      add :currency, :string, null: false
      add :status, :string, null: false, default: "pending"
      add :demo, :boolean, null: false, default: false
      add :items, {:array, :map}, null: false, default: []
      timestamps(type: :utc_datetime)
    end

    create unique_index(:orders, [:reference])

    create table(:deliveries) do
      add :order_id, references(:orders, on_delete: :nilify_all)
      add :machine_id, references(:machines, on_delete: :nilify_all)
      add :reference, :string, null: false
      add :lane, :integer, null: false
      add :qty, :integer, null: false
      add :ok, :boolean, null: false
      add :detail, :text, null: false, default: ""
      timestamps(type: :utc_datetime)
    end

    create table(:releases) do
      add :version, :string, null: false, default: ""
      add :version_code, :integer, null: false, default: 0
      add :url, :text, null: false, default: ""
      add :sha256, :string, null: false, default: ""
      add :required_version, :string, null: false, default: ""
      add :notes, :text, null: false, default: ""
      timestamps(type: :utc_datetime)
    end

    create table(:upgrade_reports) do
      add :machine_id, references(:machines, on_delete: :nilify_all)
      add :machine_no, :string, null: false, default: ""
      add :number, :string, null: false, default: ""
      add :before_version, :string, null: false, default: ""
      add :after_version, :string, null: false, default: ""
      add :state, :string, null: false, default: ""
      add :progress, :integer, null: false, default: 0
      add :error, :text, null: false, default: ""
      timestamps(type: :utc_datetime)
    end

    create table(:operators) do
      add :email, :string, null: false
      add :password_hash, :string, null: false
      timestamps(type: :utc_datetime)
    end

    create unique_index(:operators, [:email])
  end
end
