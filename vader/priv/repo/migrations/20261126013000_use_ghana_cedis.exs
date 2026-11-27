defmodule Vader.Repo.Migrations.UseGhanaCedis do
  use Ecto.Migration

  def change do
    execute "UPDATE machines SET currency = 'GHS' WHERE currency = 'NGN'",
            "UPDATE machines SET currency = 'NGN' WHERE currency = 'GHS'"

    execute "UPDATE goods SET currency = 'GHS' WHERE currency = 'NGN'",
            "UPDATE goods SET currency = 'NGN' WHERE currency = 'GHS'"

    execute "UPDATE orders SET currency = 'GHS' WHERE currency = 'NGN'",
            "UPDATE orders SET currency = 'NGN' WHERE currency = 'GHS'"

    execute "ALTER TABLE machines ALTER COLUMN currency SET DEFAULT 'GHS'",
            "ALTER TABLE machines ALTER COLUMN currency SET DEFAULT 'NGN'"

    execute "ALTER TABLE goods ALTER COLUMN currency SET DEFAULT 'GHS'",
            "ALTER TABLE goods ALTER COLUMN currency SET DEFAULT 'NGN'"
  end
end
