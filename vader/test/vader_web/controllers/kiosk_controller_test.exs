defmodule VaderWeb.KioskControllerTest do
  use VaderWeb.ConnCase, async: false

  alias Vader.{Accounts, Machines, Orders}

  setup do
    {:ok, machine} =
      Machines.create_machine(%{machine_no: "VM-TEST", name: "Test cabinet", currency: "NGN"})

    {:ok, _} =
      Machines.create_good(%{
        machine_id: machine.id,
        sku: "A1",
        name: "Still Water",
        price_minor: 20_000,
        lane: 1,
        stock: 8,
        currency: "NGN"
      })

    %{machine: machine}
  end

  test "cash stays closed until CASH_ENABLED is set", %{conn: conn, machine: machine} do
    params = %{
      "reference" => "VM-TEST-CASH",
      "amount_minor" => 20_000,
      "currency" => "NGN",
      "machine_no" => machine.machine_no,
      "items" => []
    }

    conn = post(sign_in(conn), "/api/app/payments/cash", params)
    assert json_response(conn, 404)["message"] == "Cash is not enabled"

    Application.put_env(:vader, :cash_enabled, true)
    on_exit(fn -> Application.put_env(:vader, :cash_enabled, false) end)

    assert json_response(post(build_conn(), "/api/app/payments/cash", params), 401)
    assert Orders.get_by_reference("VM-TEST-CASH") == nil

    conn = post(sign_in(build_conn()), "/api/app/payments/cash", params)
    assert %{"status" => "success", "reference" => "VM-TEST-CASH", "amount_minor" => 20_000} = json_response(conn, 200)

    assert Orders.get_by_reference("VM-TEST-CASH").payment_method == "cash"
    assert [%{total_minor: 20_000}] = Orders.sales_totals()

    refund = post(sign_in(build_conn()), "/admin/api/orders/VM-TEST-CASH/refund")
    assert %{"data" => %{"status" => "refunded"}} = json_response(refund, 200)
    assert Orders.sales_totals() == []

    again = post(sign_in(build_conn()), "/admin/api/orders/VM-TEST-CASH/refund")
    assert json_response(again, 409)
  end

  test "the database rejects a second cabinet settings row" do
    alias Vader.Settings.Cabinet

    assert Vader.Repo.aggregate(Cabinet, :count) == 1

    duplicate =
      Ecto.Changeset.change(%Cabinet{id: 2})
      |> Ecto.Changeset.check_constraint(:id, name: :cabinet_settings_singleton)

    assert {:error, changeset} = Vader.Repo.insert(duplicate, mode: :savepoint)
    assert changeset.errors[:id]
  end

  test "the desk cash switch overrides the environment", %{conn: conn, machine: machine} do
    Application.put_env(:vader, :cash_enabled, true)
    on_exit(fn -> Application.put_env(:vader, :cash_enabled, false) end)

    conn = get(sign_in(conn), "/admin/api/settings")
    status = json_response(conn, 200)["data"]
    assert status["source"] == "environment"
    assert status["cash_enabled"] == true

    conn = put(sign_in(build_conn()), "/admin/api/settings", %{cash_enabled: false})
    assert %{"cash_enabled" => false, "source" => "desk"} = json_response(conn, 200)["data"]

    closed =
      post(sign_in(build_conn()), "/api/app/payments/cash", %{
        "reference" => "VM-TEST-CASH-DESK-OFF",
        "amount_minor" => 20_000,
        "currency" => "NGN",
        "machine_no" => machine.machine_no,
        "items" => []
      })

    assert json_response(closed, 404)["message"] == "Cash is not enabled"

    conn = put(sign_in(build_conn()), "/admin/api/settings", %{cash_enabled: true})
    assert %{"cash_enabled" => true, "source" => "desk"} = json_response(conn, 200)["data"]

    opened =
      post(sign_in(build_conn()), "/api/app/payments/cash", %{
        "reference" => "VM-TEST-CASH-DESK-ON",
        "amount_minor" => 20_000,
        "currency" => "NGN",
        "machine_no" => machine.machine_no,
        "items" => []
      })

    assert %{"status" => "success"} = json_response(opened, 200)
  end

  test "lists goods for a known machine", %{conn: conn} do
    conn = get(conn, "/api/app/v1/goods/getGoodsList?machine_no=VM-TEST")
    assert %{"code" => 0, "data" => [%{"id" => "A1", "lane" => 1, "price_minor" => 20_000}]} = json_response(conn, 200)
  end

  test "demo checkout can be confirmed", %{conn: conn} do
    conn =
      post(conn, "/api/app/payments/paystack/initialize", %{
        "reference" => "VM-TEST-1",
        "email" => "payments@example.com",
        "amount_minor" => 20_000,
        "currency" => "NGN",
        "machine_no" => "VM-TEST",
        "items" => []
      })

    assert %{"demo" => true, "reference" => "VM-TEST-1"} = json_response(conn, 200)

    conn = post(build_conn(), "/pay/demo/confirm", %{"reference" => "VM-TEST-1"})
    assert %{"status" => "success"} = json_response(conn, 200)

    conn = get(build_conn(), "/api/app/payments/paystack/verify?reference=VM-TEST-1")
    assert %{"status" => "success", "demo" => true} = json_response(conn, 200)
  end

  test "admin routes reject a missing token", %{conn: conn} do
    conn = get(conn, "/admin/api/machines")
    assert json_response(conn, 401)["message"] == "Sign in required"
  end

  test "overview counts lanes and paid sales", %{conn: conn, machine: machine} do
    {:ok, _} =
      Orders.create_order(%{
        machine_id: machine.id,
        reference: "VM-TEST-PAID",
        email: "payments@example.com",
        amount_minor: 20_000,
        currency: "NGN",
        status: "success",
        demo: true,
        items: [%{"name" => "Still Water", "qty" => 1, "lane" => 1}]
      })

    conn = get(sign_in(conn), "/admin/api/overview")
    data = json_response(conn, 200)["data"]

    assert data["machines"] == 1
    assert data["lanes"] == 1
    assert data["empty_lanes"] == 0
    assert data["paystack"] in ["demo", "live"]
    assert [%{"currency" => "NGN", "total_minor" => 20_000, "today_minor" => 20_000}] = data["sales"]
  end

  test "a cabinet can file a coil walk", %{conn: conn} do
    conn =
      post(conn, "/api/app/v1/machine/commission", %{
        "machine_no" => "VM-TEST",
        "board" => "vendele",
        "serial_device" => "/dev/ttyS1",
        "baud" => 38400,
        "rows" => 1,
        "coils" => 2,
        "simulate" => false,
        "payment_lane" => 1,
        "payment_reference" => "VM-TEST-PAY",
        "payment_status" => "success",
        "checks" => [
          %{"lane" => 1, "row" => 1, "coil" => 1, "result" => "turned", "detail" => "motor ran"},
          %{"lane" => 2, "row" => 1, "coil" => 2, "result" => "stuck", "detail" => ""}
        ]
      })

    assert %{"code" => 0, "data" => %{"id" => id, "turned" => 1, "stuck" => 1, "machine_no" => "VM-TEST", "status" => "draft"}} =
             json_response(conn, 200)

    updated =
      post(build_conn(), "/api/app/v1/machine/commission", %{
        "machine_no" => "VM-TEST",
        "board" => "vendele",
        "serial_device" => "/dev/ttyS1",
        "baud" => 38400,
        "rows" => 1,
        "coils" => 2,
        "simulate" => false,
        "checks" => [
          %{"lane" => 1, "row" => 1, "coil" => 1, "result" => "turned", "detail" => "motor ran"},
          %{"lane" => 2, "row" => 1, "coil" => 2, "result" => "empty", "detail" => ""}
        ]
      })

    assert %{"code" => 0, "data" => %{"id" => ^id, "empty" => 1, "status" => "draft"}} =
             json_response(updated, 200)

    finished =
      post(build_conn(), "/api/app/v1/machine/commission", %{
        "machine_no" => "VM-TEST",
        "board" => "vendele",
        "serial_device" => "/dev/ttyS1",
        "baud" => 38400,
        "rows" => 1,
        "coils" => 2,
        "finalize" => true,
        "payment_lane" => 1,
        "payment_reference" => "VM-TEST-PAY",
        "payment_status" => "success",
        "checks" => [
          %{"lane" => 1, "row" => 1, "coil" => 1, "result" => "turned", "detail" => "motor ran"}
        ]
      })

    assert %{"code" => 0, "data" => %{"id" => ^id, "status" => "saved"}} = json_response(finished, 200)

    conn = get(sign_in(build_conn()), "/admin/api/commission")
    assert [%{"payment_status" => "success", "baud" => 38400, "status" => "saved"}] = json_response(conn, 200)["data"]
  end

  test "a paid order can be refunded from the desk", %{conn: conn, machine: machine} do
    {:ok, _} =
      Orders.create_order(%{
        machine_id: machine.id,
        reference: "VM-TEST-REFUND",
        email: "payments@example.com",
        amount_minor: 20_000,
        currency: "NGN",
        status: "success",
        demo: true,
        items: []
      })

    conn = post(sign_in(conn), "/admin/api/orders/VM-TEST-REFUND/refund")
    assert %{"data" => %{"status" => "refunded"}} = json_response(conn, 200)
  end

  test "the desk switches temperature and humidity for the cabinet", %{conn: conn} do
    conn = get(sign_in(conn), "/admin/api/settings")
    assert %{"temperature_enabled" => false, "humidity_enabled" => false} = json_response(conn, 200)["data"]

    conn = put(sign_in(build_conn()), "/admin/api/settings", %{temperature_enabled: true, humidity_enabled: true})
    assert %{"temperature_enabled" => true, "humidity_enabled" => true, "temperature_source" => "desk"} =
             json_response(conn, 200)["data"]

    health = get(build_conn(), "/health")
    assert %{"temperature" => true, "humidity" => true} = json_response(health, 200)

    posted =
      post(build_conn(), "/api/app/v1/machine/climate", %{
        "temperature_c" => 4.2,
        "humidity_percent" => 55
      })

    assert %{"temperature_c" => 4.2, "humidity_percent" => 55.0} = json_response(posted, 200)["data"]

    conn = put(sign_in(build_conn()), "/admin/api/settings", %{temperature_enabled: false})
    turned_off = json_response(conn, 200)["data"]
    assert turned_off["temperature_enabled"] == false
    assert turned_off["temperature_c"] == nil
    assert turned_off["humidity_enabled"] == true
    assert turned_off["humidity_percent"] == 55.0

    ignored =
      post(build_conn(), "/api/app/v1/machine/climate", %{
        "temperature_c" => 9.6,
        "humidity_percent" => 40
      })

    stored = json_response(ignored, 200)["data"]
    assert stored["temperature_c"] == nil
    assert stored["humidity_percent"] == 40.0
  end

  defp sign_in(conn) do
    {:ok, operator} = Accounts.ensure_operator("desk@localhost", "desk-pass1")
    token = Accounts.sign(operator)
    put_req_header(conn, "authorization", "Bearer " <> token)
  end
end
