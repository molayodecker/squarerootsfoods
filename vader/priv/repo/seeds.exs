alias Vader.Accounts
alias Vader.Brands
alias Vader.Fleet
alias Vader.Machines

email = Application.get_env(:vader, :admin_email, "operator@localhost")
password = Application.get_env(:vader, :admin_password, "change-me")

case Accounts.ensure_operator(email, password) do
  {:ok, _} -> IO.puts("Operator ready: #{email}")
  {:error, reason} -> IO.inspect(reason, label: "operator seed failed")
end

{:ok, machine} =
  case Machines.get_by_no("VM-001") do
    nil ->
      Machines.create_machine(%{machine_no: "VM-001", name: "Bench cabinet", currency: "GHS"})

    existing ->
      {:ok, existing}
  end

case Machines.get_by_no("19265359") do
  nil ->
    Machines.create_machine(%{machine_no: "19265359", name: "Cabinet", currency: "GHS"})

  _existing ->
    :ok
end

if Machines.list_goods(machine.id) == [] do
  [
    %{sku: "A1", name: "Still Water", price_minor: 20_000, lane: 1, stock: 8},
    %{sku: "A2", name: "Coca-Cola", price_minor: 35_000, lane: 2, stock: 6},
    %{sku: "A3", name: "Fanta", price_minor: 35_000, lane: 3, stock: 8},
    %{sku: "A4", name: "Sprite", price_minor: 35_000, lane: 4, stock: 8},
    %{sku: "B1", name: "Potato Chips", price_minor: 25_000, lane: 5, stock: 10},
    %{sku: "B2", name: "Chocolate Bar", price_minor: 15_000, lane: 6, stock: 12}
  ]
  |> Enum.each(fn attrs ->
    {:ok, _} =
      Machines.create_good(Map.merge(attrs, %{machine_id: machine.id, currency: machine.currency}))
  end)
end

:ok = Brands.import!()
_ = Fleet.current_release()
