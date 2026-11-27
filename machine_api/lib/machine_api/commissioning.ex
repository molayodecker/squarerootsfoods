defmodule MachineApi.Commissioning do
  import Ecto.Query
  alias MachineApi.Commissioning.{LaneCheck, Report}
  alias MachineApi.{Machines, Repo}

  def record(params) when is_map(params) do
    Repo.transaction(fn ->
      machine = ensure_machine(params)
      checks = normalize_checks(params["checks"])

      if checks == :too_many do
        Repo.rollback("Too many lanes")
      end

      finalize = params["finalize"] in [true, "true"]

      attrs = %{
        machine_id: machine.id,
        board: board(params["board"]),
        serial_device: text(params["serial_device"]),
        baud: int(params["baud"], 9600),
        rows: int(params["rows"]),
        coils: int(params["coils"]),
        simulate: params["simulate"] in [true, "true", 1],
        payment_lane: int(params["payment_lane"]),
        payment_reference: text(params["payment_reference"]),
        payment_status: text(params["payment_status"]),
        status: if(finalize, do: "saved", else: "draft")
      }

      report =
        case open_draft(machine.id) do
          nil ->
            case %Report{} |> Report.changeset(attrs) |> Repo.insert() do
              {:ok, report} -> report
              {:error, changeset} -> Repo.rollback(changeset)
            end

          draft ->
            case draft |> Report.changeset(attrs) |> Repo.update() do
              {:ok, report} ->
                Repo.delete_all(from c in LaneCheck, where: c.report_id == ^report.id)
                report

              {:error, changeset} ->
                Repo.rollback(changeset)
            end
        end

      Enum.each(checks, fn check ->
        %LaneCheck{}
        |> LaneCheck.changeset(Map.put(check, :report_id, report.id))
        |> Repo.insert!()
      end)

      Repo.preload(report, [:machine, :checks], force: true)
    end)
  end

  defp open_draft(machine_id) do
    Repo.one(
      from r in Report,
        where: r.machine_id == ^machine_id and r.status == "draft",
        order_by: [desc: r.id],
        limit: 1
    )
  end

  def latest(limit \\ 20) do
    Repo.all(
      from r in Report,
        order_by: [desc: r.inserted_at],
        limit: ^limit,
        preload: [:machine, :checks]
    )
  end

  def json(%Report{} = report) do
    checks = Enum.sort_by(report.checks, & &1.lane)

    %{
      id: report.id,
      machine_no: machine_no(report),
      board: report.board,
      serial_device: report.serial_device,
      baud: report.baud,
      rows: report.rows,
      coils: report.coils,
      simulate: report.simulate,
      payment_lane: report.payment_lane,
      payment_reference: report.payment_reference,
      payment_status: report.payment_status,
      status: report.status,
      turned: Enum.count(checks, &(&1.result == "turned")),
      stuck: Enum.count(checks, &(&1.result == "stuck")),
      empty: Enum.count(checks, &(&1.result == "empty")),
      untested: Enum.count(checks, &(&1.result == "untested")),
      checks:
        Enum.map(checks, fn check ->
          %{
            lane: check.lane,
            row: check.row,
            coil: check.coil,
            result: check.result,
            detail: check.detail
          }
        end),
      inserted_at: report.inserted_at,
      updated_at: report.updated_at
    }
  end

  defp ensure_machine(params) do
    number = text(params["machine_no"])

    cond do
      number == "" ->
        Repo.rollback("machine_no is required")

      machine = Machines.get_by_no(number) ->
        machine

      true ->
        case Machines.create_machine(%{
               machine_no: number,
               name: text(params["name"]) |> then(fn name -> if(name == "", do: number, else: name) end),
               currency: text(params["currency"]) |> then(fn code -> if(code == "", do: "GHS", else: code) end)
             }) do
          {:ok, machine} -> machine
          {:error, changeset} -> Repo.rollback(changeset)
        end
    end
  end

  defp normalize_checks(checks) when is_list(checks) and length(checks) > 400, do: :too_many

  defp normalize_checks(checks) when is_list(checks) do
    Enum.map(checks, fn check ->
      result = text(check["result"])

      %{
        lane: int(check["lane"]),
        row: int(check["row"]),
        coil: int(check["coil"]),
        result: if(result in ["turned", "stuck", "empty", "untested"], do: result, else: "untested"),
        detail: text(check["detail"])
      }
    end)
  end

  defp normalize_checks(_), do: []

  defp board("vendele"), do: "vendele"
  defp board(_), do: "unknown"

  defp machine_no(%{machine: %{machine_no: number}}), do: number
  defp machine_no(_), do: nil

  defp text(value) when is_binary(value), do: String.trim(value)
  defp text(_), do: ""

  defp int(value, default \\ 0)
  defp int(value, _) when is_integer(value), do: value

  defp int(value, default) when is_binary(value) do
    case Integer.parse(value) do
      {number, _} -> number
      :error -> default
    end
  end

  defp int(_, default), do: default
end
