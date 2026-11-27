defmodule VaderWeb.KioskControllerTest do
  use VaderWeb.ConnCase, async: true

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

  defp sign_in(conn) do
    {:ok, operator} = Accounts.ensure_operator("desk@localhost", "desk-pass1")
    token = Accounts.sign(operator)
    put_req_header(conn, "authorization", "Bearer " <> token)
  end
end
