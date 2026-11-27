defmodule Vader.Repo.Migrations.HardenCabinetSettingsAndCashOrders do
  use Ecto.Migration

  def up do
    execute("""
    DELETE FROM cabinet_settings
    WHERE id <> (SELECT id FROM cabinet_settings ORDER BY updated_at DESC, id DESC LIMIT 1)
    """)

    execute("UPDATE cabinet_settings SET id = 1 WHERE id <> 1")

    execute("""
    INSERT INTO cabinet_settings (id, inserted_at, updated_at)
    VALUES (1, now(), now())
    ON CONFLICT (id) DO NOTHING
    """)

    create constraint(:cabinet_settings, :cabinet_settings_singleton, check: "id = 1")

    alter table(:orders) do
      add :payment_method, :string, null: false, default: "paystack"
    end

    execute("UPDATE orders SET payment_method = 'cash' WHERE email = 'cash@machine' AND demo = false")
  end

  def down do
    alter table(:orders) do
      remove :payment_method
    end

    drop constraint(:cabinet_settings, :cabinet_settings_singleton)
  end
end
