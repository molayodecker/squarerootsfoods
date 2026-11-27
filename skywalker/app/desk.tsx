"use client";

import { FormEvent, useEffect, useState } from "react";

const apiBase = process.env.NEXT_PUBLIC_API_URL || "http://127.0.0.1:8080";

type Machine = {
  id: number;
  machine_no: string;
  name: string;
  currency: string;
  lanes: number;
  empty_lanes: number;
};
type Good = {
  record_id: number;
  id: string;
  machine_id: number;
  machine_no?: string;
  machine_name?: string;
  name: string;
  price_minor: number;
  lane: number;
  stock: number;
  currency: string;
  image_url?: string;
  category?: string;
};
type OrderItem = { name?: string; qty?: number; lane?: number };
type Delivery = { lane: number; qty: number; ok: boolean; detail: string };
type Order = {
  id: number;
  reference: string;
  email: string;
  amount_minor: number;
  currency: string;
  status: string;
  demo: boolean;
  machine_no: string | null;
  items: OrderItem[];
  deliveries: Delivery[];
  inserted_at: string;
};
type Release = {
  version: string;
  version_code: number;
  url: string;
  sha256: string;
  required_version: string;
  notes: string;
  update: boolean;
};
type Sales = { currency: string; today_minor: number; month_minor: number; total_minor: number };
type Overview = {
  machines: number;
  lanes: number;
  empty_lanes: number;
  paystack: "demo" | "live";
  sales: Sales[];
};
type Payments = { provider: string; mode: "demo" | "live"; public_base_url: string };
type CoilCheck = { lane: number; row: number; coil: number; result: string; detail: string };
type InstallReport = {
  id: number;
  machine_no: string | null;
  board: string;
  serial_device: string;
  baud: number;
  rows: number;
  coils: number;
  simulate: boolean;
  payment_lane: number;
  payment_reference: string;
  payment_status: string;
  status: string;
  turned: number;
  stuck: number;
  empty: number;
  untested: number;
  checks: CoilCheck[];
  inserted_at: string;
};

type Brand = {
  id: number;
  slug: string;
  name: string;
  category: string;
  image_url: string;
};
type View = "overview" | "devices" | "goods" | "brands" | "orders" | "payments" | "install" | "release";

async function call<T>(path: string, options: RequestInit = {}): Promise<T> {
  const token = window.localStorage.getItem("machine_token");
  const headers = new Headers(options.headers);
  headers.set("Content-Type", "application/json");
  if (token) headers.set("Authorization", `Bearer ${token}`);
  const response = await fetch(`${apiBase}${path}`, { ...options, headers });
  const body = await response.json().catch(() => ({}));
  if (!response.ok) {
    throw new Error(body.message || `Request failed (${response.status})`);
  }
  return body as T;
}

function money(minor: number, currency: string) {
  const abs = Math.abs(minor);
  const label = currency === "GHS" ? "GH₵" : currency;
  return `${label} ${Math.floor(abs / 100)}.${String(abs % 100).padStart(2, "0")}`;
}

function when(value: string) {
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString();
}

export function Desk() {
  const [token, setToken] = useState<string | null>(null);
  const [view, setView] = useState<View>("overview");
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    setToken(window.localStorage.getItem("machine_token"));
  }, []);

  if (!token) {
    return <Login onReady={() => setToken(window.localStorage.getItem("machine_token"))} />;
  }

  return (
    <div className="mx-auto grid min-h-screen max-w-6xl grid-cols-[240px_1fr]">
      <aside className="bg-[#003366] px-5 py-8 text-[#fff4e8]">
        <img src="/logo-reversed.png" alt="Square Roots Foods" className="h-auto w-full" />
        <h1 className="mt-4 text-lg font-medium">Desk</h1>
        <nav className="mt-10 grid gap-2">
          <NavButton current={view} id="overview" onClick={setView} label="Overview" />
          <NavButton current={view} id="devices" onClick={setView} label="Devices" />
          <NavButton current={view} id="goods" onClick={setView} label="Goods" />
          <NavButton current={view} id="brands" onClick={setView} label="Brands" />
          <NavButton current={view} id="orders" onClick={setView} label="Orders" />
          <NavButton current={view} id="payments" onClick={setView} label="Payments" />
          <NavButton current={view} id="install" onClick={setView} label="Install" />
          <NavButton current={view} id="release" onClick={setView} label="App update" />
        </nav>
        <button
          className="mt-10 text-sm text-[#fff4e8]/80"
          onClick={() => {
            window.localStorage.removeItem("machine_token");
            setToken(null);
          }}
        >
          Sign out
        </button>
      </aside>
      <main className="px-8 py-8">
        {error ? <p className="mb-4 text-[#ff8a80]">{error}</p> : null}
        {view === "overview" ? <OverviewPage onError={setError} /> : null}
        {view === "devices" ? <Machines onError={setError} /> : null}
        {view === "goods" ? <GoodsPage onError={setError} /> : null}
        {view === "brands" ? <BrandsPage onError={setError} /> : null}
        {view === "orders" ? <Orders onError={setError} /> : null}
        {view === "payments" ? <PaymentsPage onError={setError} /> : null}
        {view === "install" ? <InstallPage onError={setError} /> : null}
        {view === "release" ? <ReleaseForm onError={setError} /> : null}
      </main>
    </div>
  );
}

function NavButton({
  current,
  id,
  label,
  onClick,
}: {
  current: View;
  id: View;
  label: string;
  onClick: (view: View) => void;
}) {
  return (
    <button
      className={`rounded-lg px-3 py-2 text-left font-medium ${current === id ? "bg-white/15" : "hover:bg-white/10"}`}
      onClick={() => onClick(id)}
    >
      {label}
    </button>
  );
}

function Login({ onReady }: { onReady: () => void }) {
  const [email, setEmail] = useState("operator@localhost");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);

  async function submit(event: FormEvent) {
    event.preventDefault();
    setError(null);
    try {
      const body = await call<{ token: string }>("/admin/api/session", {
        method: "POST",
        body: JSON.stringify({ email, password }),
      });
      window.localStorage.setItem("machine_token", body.token);
      onReady();
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : "Sign in failed");
    }
  }

  return (
    <main className="mx-auto flex min-h-screen max-w-md flex-col justify-center px-6">
      <img src="/logo-reversed.png" alt="Square Roots Foods" className="h-14 w-auto" />
      <h1 className="mt-6 text-3xl font-medium">Sign in</h1>
      <form className="mt-8 grid gap-4" onSubmit={submit}>
        <label className="grid gap-1 text-sm">
          Email
          <input className="rounded-lg border border-[#6699cc]/40 bg-[#0b3d6e] px-3 py-2" value={email} onChange={(event) => setEmail(event.target.value)} />
        </label>
        <label className="grid gap-1 text-sm">
          Password
          <input className="rounded-lg border border-[#6699cc]/40 bg-[#0b3d6e] px-3 py-2" type="password" value={password} onChange={(event) => setPassword(event.target.value)} />
        </label>
        {error ? <p className="text-[#ff8a80]">{error}</p> : null}
        <button className="rounded-lg bg-[#ff6600] text-[#fff4e8] px-4 py-3 text-white" type="submit">
          Continue
        </button>
      </form>
    </main>
  );
}

function OverviewPage({ onError }: { onError: (message: string | null) => void }) {
  const [overview, setOverview] = useState<Overview | null>(null);

  useEffect(() => {
    call<{ data: Overview }>("/admin/api/overview")
      .then((body) => setOverview(body.data))
      .catch((reason) => onError(reason.message));
  }, [onError]);

  if (!overview) return <p>Loading the desk…</p>;

  const sales = overview.sales.length > 0 ? overview.sales : [{ currency: "GHS", today_minor: 0, month_minor: 0, total_minor: 0 }];

  return (
    <section>
      <h2 className="text-2xl font-medium">Overview</h2>
      <div className="mt-4 grid gap-3 sm:grid-cols-3">
        <Stat label="Devices" value={String(overview.machines)} />
        <Stat label="Lanes" value={String(overview.lanes)} />
        <Stat label="Empty lanes" value={String(overview.empty_lanes)} />
      </div>
      <div className="mt-6 grid gap-3">
        {sales.map((row) => (
          <div key={row.currency} className="grid gap-3 rounded-xl bg-[#0b3d6e] p-4 sm:grid-cols-3">
            <Stat label="Today" value={money(row.today_minor, row.currency)} />
            <Stat label="This month" value={money(row.month_minor, row.currency)} />
            <Stat label="Paid total" value={money(row.total_minor, row.currency)} />
          </div>
        ))}
      </div>
      <p className="mt-4 text-sm text-[#b8cfe8]">
        Paystack is {overview.paystack === "live" ? "live" : "in demo mode"}. Totals count paid orders only.
      </p>
    </section>
  );
}

function Stat({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-xl bg-[#0b3d6e] px-4 py-3">
      <p className="text-sm text-[#b8cfe8]">{label}</p>
      <p className="mt-1 text-2xl font-medium">{value}</p>
    </div>
  );
}

function Machines({ onError }: { onError: (message: string | null) => void }) {
  const [machines, setMachines] = useState<Machine[]>([]);
  const [selected, setSelected] = useState<Machine | null>(null);
  const [machineNo, setMachineNo] = useState("");
  const [name, setName] = useState("");
  const [currency, setCurrency] = useState("GHS");

  async function load() {
    const body = await call<{ data: Machine[] }>("/admin/api/machines");
    setMachines(body.data);
  }

  useEffect(() => {
    load().catch((reason) => onError(reason.message));
  }, [onError]);

  async function create(event: FormEvent) {
    event.preventDefault();
    onError(null);
    try {
      await call("/admin/api/machines", {
        method: "POST",
        body: JSON.stringify({ machine_no: machineNo, name, currency }),
      });
      setMachineNo("");
      setName("");
      await load();
    } catch (reason) {
      onError(reason instanceof Error ? reason.message : "Could not add the machine");
    }
  }

  return (
    <section>
      <h2 className="text-2xl font-medium">Devices</h2>
      <form className="mt-4 flex flex-wrap gap-2" onSubmit={create}>
        <input className="rounded-lg border border-[#6699cc]/40 bg-[#0b3d6e] px-3 py-2" placeholder="Machine number" value={machineNo} onChange={(event) => setMachineNo(event.target.value)} />
        <input className="rounded-lg border border-[#6699cc]/40 bg-[#0b3d6e] px-3 py-2" placeholder="Name" value={name} onChange={(event) => setName(event.target.value)} />
        <input className="w-24 rounded-lg border border-[#6699cc]/40 bg-[#0b3d6e] px-3 py-2" value={currency} onChange={(event) => setCurrency(event.target.value)} />
        <button className="rounded-lg bg-[#ff6600] text-[#fff4e8] px-4 py-2 text-white" type="submit">
          Add
        </button>
      </form>
      <table className="mt-6 w-full overflow-hidden rounded-xl bg-[#0b3d6e] text-left">
        <thead className="text-sm text-[#b8cfe8]">
          <tr>
            <th className="px-4 py-2">Number</th>
            <th className="px-4 py-2">Name</th>
            <th className="px-4 py-2">Currency</th>
            <th className="px-4 py-2">Lanes</th>
            <th className="px-4 py-2">Empty</th>
          </tr>
        </thead>
        <tbody>
          {machines.map((machine) => (
            <tr key={machine.id} className="border-t border-[#6699cc]/40">
              <td className="px-4 py-3">
                <button className="font-medium" onClick={() => setSelected(machine)}>
                  {machine.machine_no}
                </button>
              </td>
              <td className="px-4 py-3">{machine.name}</td>
              <td className="px-4 py-3">{machine.currency}</td>
              <td className="px-4 py-3">{machine.lanes}</td>
              <td className="px-4 py-3">{machine.empty_lanes}</td>
            </tr>
          ))}
        </tbody>
      </table>
      {selected ? <LaneEditor machine={selected} onError={onError} /> : null}
    </section>
  );
}

function LaneEditor({ machine, onError }: { machine: Machine; onError: (message: string | null) => void }) {
  const [goods, setGoods] = useState<Good[]>([]);
  const [sku, setSku] = useState("");
  const [name, setName] = useState("");
  const [lane, setLane] = useState("1");
  const [price, setPrice] = useState("200");
  const [stock, setStock] = useState("8");
  const [brands, setBrands] = useState<Brand[]>([]);
  const [brandSlug, setBrandSlug] = useState("");

  async function load() {
    const body = await call<{ data: Good[] }>(`/admin/api/goods?machine_id=${machine.id}`);
    setGoods(body.data);
  }

  useEffect(() => {
    load().catch((reason) => onError(reason.message));
    call<{ data: Brand[] }>("/admin/api/brands")
      .then((body) => setBrands(body.data))
      .catch((reason) => onError(reason.message));
  }, [machine.id, onError]);

  async function create(event: FormEvent) {
    event.preventDefault();
    onError(null);
    try {
      await call("/admin/api/goods", {
        method: "POST",
        body: JSON.stringify({
          machine_id: machine.id,
          sku,
          name,
          lane: Number(lane),
          price_minor: Math.round(Number(price) * 100),
          stock: Number(stock),
          currency: machine.currency,
          brand_slug: brandSlug,
        }),
      });
      setSku("");
      setName("");
      setBrandSlug("");
      await load();
    } catch (reason) {
      onError(reason instanceof Error ? reason.message : "Could not add the product");
    }
  }

  async function saveStock(good: Good, next: number) {
    onError(null);
    try {
      await call(`/admin/api/goods/${good.record_id}`, {
        method: "PATCH",
        body: JSON.stringify({ stock: next }),
      });
      await load();
    } catch (reason) {
      onError(reason instanceof Error ? reason.message : "Could not update stock");
    }
  }

  return (
    <div className="mt-8">
      <h3 className="text-xl font-medium">Lanes on {machine.machine_no}</h3>
      <form className="mt-3 flex flex-wrap items-center gap-2" onSubmit={create}>
        <select
          className="rounded-lg border border-[#6699cc]/40 bg-[#0b3d6e] px-3 py-2"
          value={brandSlug}
          onChange={(event) => {
            const slug = event.target.value;
            setBrandSlug(slug);
            const brand = brands.find((item) => item.slug === slug);
            if (brand) setName(brand.name);
          }}
        >
          <option value="">Brand</option>
          {brands.map((brand) => (
            <option key={brand.slug} value={brand.slug}>
              {brand.name}
            </option>
          ))}
        </select>
        {brandSlug ? (
          <img
            src={`${apiBase}${brands.find((brand) => brand.slug === brandSlug)?.image_url || ""}`}
            alt=""
            className="h-12 w-12 rounded-md bg-[#002346] object-contain"
          />
        ) : null}
        <input className="w-20 rounded-lg border border-[#6699cc]/40 bg-[#0b3d6e] px-3 py-2" placeholder="SKU" value={sku} onChange={(event) => setSku(event.target.value)} />
        <input className="rounded-lg border border-[#6699cc]/40 bg-[#0b3d6e] px-3 py-2" placeholder="Name" value={name} onChange={(event) => setName(event.target.value)} />
        <input className="w-20 rounded-lg border border-[#6699cc]/40 bg-[#0b3d6e] px-3 py-2" placeholder="Lane" value={lane} onChange={(event) => setLane(event.target.value)} />
        <input className="w-28 rounded-lg border border-[#6699cc]/40 bg-[#0b3d6e] px-3 py-2" placeholder="Price" value={price} onChange={(event) => setPrice(event.target.value)} />
        <input className="w-20 rounded-lg border border-[#6699cc]/40 bg-[#0b3d6e] px-3 py-2" placeholder="Stock" value={stock} onChange={(event) => setStock(event.target.value)} />
        <button className="rounded-lg bg-[#ff6600] text-[#fff4e8] px-4 py-2 text-white" type="submit">
          Add lane
        </button>
      </form>
      <GoodsTable goods={goods} onStock={saveStock} />
    </div>
  );
}

function BrandsPage({ onError }: { onError: (message: string | null) => void }) {
  const [brands, setBrands] = useState<Brand[]>([]);

  useEffect(() => {
    call<{ data: Brand[] }>("/admin/api/brands")
      .then((body) => setBrands(body.data))
      .catch((reason) => onError(reason.message));
  }, [onError]);

  return (
    <section>
      <h2 className="text-2xl font-medium">Brands</h2>
      <p className="mt-2 text-sm text-[#b8cfe8]">Pick a brand when you add a lane. The item keeps that photo.</p>
      <div className="mt-6 grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-4">
        {brands.map((brand) => (
          <article key={brand.slug} className="rounded-xl bg-[#0b3d6e] p-3">
            <img src={`${apiBase}${brand.image_url}`} alt={brand.name} className="aspect-square w-full rounded-lg bg-[#002346] object-contain" />
            <p className="mt-2 font-medium">{brand.name}</p>
            <p className="text-sm text-[#8fb3da]">{brand.category}</p>
          </article>
        ))}
      </div>
    </section>
  );
}

function GoodsPage({ onError }: { onError: (message: string | null) => void }) {
  const [goods, setGoods] = useState<Good[]>([]);
  const [emptyOnly, setEmptyOnly] = useState(false);

  async function load() {
    const body = await call<{ data: Good[] }>("/admin/api/goods");
    setGoods(body.data);
  }

  useEffect(() => {
    load().catch((reason) => onError(reason.message));
  }, [onError]);

  async function save(good: Good, patch: { stock?: number; price_minor?: number }) {
    onError(null);
    try {
      await call(`/admin/api/goods/${good.record_id}`, {
        method: "PATCH",
        body: JSON.stringify(patch),
      });
      await load();
    } catch (reason) {
      onError(reason instanceof Error ? reason.message : "Could not update the product");
    }
  }

  const rows = emptyOnly ? goods.filter((good) => good.stock <= 0) : goods;

  return (
    <section>
      <div className="flex items-center justify-between gap-4">
        <h2 className="text-2xl font-medium">Goods</h2>
        <label className="flex items-center gap-2 text-sm">
          <input type="checkbox" checked={emptyOnly} onChange={(event) => setEmptyOnly(event.target.checked)} />
          Empty lanes only
        </label>
      </div>
      <GoodsTable goods={rows} showMachine onStock={(good, stock) => save(good, { stock })} onPrice={(good, price) => save(good, { price_minor: price })} />
    </section>
  );
}

function GoodsTable({
  goods,
  showMachine = false,
  onStock,
  onPrice,
}: {
  goods: Good[];
  showMachine?: boolean;
  onStock: (good: Good, stock: number) => void;
  onPrice?: (good: Good, priceMinor: number) => void;
}) {
  return (
    <table className="mt-4 w-full overflow-hidden rounded-xl bg-[#0b3d6e] text-left">
      <thead className="text-sm text-[#b8cfe8]">
        <tr>
          {showMachine ? <th className="px-4 py-2">Device</th> : null}
          <th className="px-4 py-2">Photo</th>
          <th className="px-4 py-2">Lane</th>
          <th className="px-4 py-2">SKU</th>
          <th className="px-4 py-2">Name</th>
          <th className="px-4 py-2">Price</th>
          <th className="px-4 py-2">Stock</th>
        </tr>
      </thead>
      <tbody>
        {goods.map((good) => (
          <tr key={good.record_id} className={`border-t border-[#6699cc]/40 ${good.stock <= 0 ? "bg-[#4d1d00]" : ""}`}>
            {showMachine ? <td className="px-4 py-3">{good.machine_no}</td> : null}
            <td className="px-4 py-3">
              {good.image_url ? (
                <img src={`${apiBase}${good.image_url}`} alt="" className="h-12 w-12 rounded-md bg-[#002346] object-contain" />
              ) : null}
            </td>
            <td className="px-4 py-3">{good.lane}</td>
            <td className="px-4 py-3">{good.id}</td>
            <td className="px-4 py-3">{good.name}</td>
            <td className="px-4 py-3">
              {onPrice ? (
                <input
                  className="w-24 rounded border border-[#6699cc]/40 px-2 py-1"
                  defaultValue={(good.price_minor / 100).toFixed(2)}
                  onBlur={(event) => onPrice(good, Math.round(Number(event.target.value) * 100))}
                />
              ) : (
                money(good.price_minor, good.currency)
              )}
            </td>
            <td className="px-4 py-3">
              <input
                className="w-20 rounded border border-[#6699cc]/40 px-2 py-1"
                defaultValue={good.stock}
                onBlur={(event) => onStock(good, Number(event.target.value))}
              />
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}

function Orders({ onError }: { onError: (message: string | null) => void }) {
  const [orders, setOrders] = useState<Order[]>([]);
  const [pending, setPending] = useState<string | null>(null);

  async function load() {
    const body = await call<{ data: Order[] }>("/admin/api/orders");
    setOrders(body.data);
  }

  useEffect(() => {
    load().catch((reason) => onError(reason.message));
  }, [onError]);

  async function refund(order: Order) {
    onError(null);
    setPending(order.reference);
    try {
      await call(`/admin/api/orders/${encodeURIComponent(order.reference)}/refund`, { method: "POST" });
      await load();
    } catch (reason) {
      onError(reason instanceof Error ? reason.message : "Could not refund the order");
    } finally {
      setPending(null);
    }
  }

  return (
    <section>
      <h2 className="text-2xl font-medium">Orders</h2>
      <table className="mt-4 w-full rounded-xl bg-[#0b3d6e] text-left">
        <thead className="text-sm text-[#b8cfe8]">
          <tr>
            <th className="px-4 py-2">When</th>
            <th className="px-4 py-2">Reference</th>
            <th className="px-4 py-2">Machine</th>
            <th className="px-4 py-2">Items</th>
            <th className="px-4 py-2">Amount</th>
            <th className="px-4 py-2">Status</th>
          </tr>
        </thead>
        <tbody>
          {orders.map((order) => {
            const failed = order.deliveries?.some((delivery) => !delivery.ok);
            return (
              <tr key={order.id} className="border-t border-[#6699cc]/40 align-top">
                <td className="px-4 py-3">{when(order.inserted_at)}</td>
                <td className="px-4 py-3">{order.reference}</td>
                <td className="px-4 py-3">{order.machine_no || "—"}</td>
                <td className="px-4 py-3">
                  {(order.items || []).map((item) => `${item.name || "Item"} × ${item.qty || 1}`).join(", ") || "—"}
                  {failed ? <p className="text-[#ff8a80]">Vend failed</p> : null}
                </td>
                <td className="px-4 py-3">{money(order.amount_minor, order.currency)}</td>
                <td className="px-4 py-3">
                  <p>
                    {order.status}
                    {order.demo ? " · demo" : ""}
                  </p>
                  {order.status === "success" ? (
                    <button
                      className="mt-2 rounded-lg border border-[#6699cc]/40 px-3 py-1 text-sm"
                      disabled={pending === order.reference}
                      onClick={() => refund(order)}
                    >
                      Refund
                    </button>
                  ) : null}
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </section>
  );
}

function PaymentsPage({ onError }: { onError: (message: string | null) => void }) {
  const [payments, setPayments] = useState<Payments | null>(null);

  useEffect(() => {
    call<{ data: Payments }>("/admin/api/payments")
      .then((body) => setPayments(body.data))
      .catch((reason) => onError(reason.message));
  }, [onError]);

  if (!payments) return <p>Loading payments…</p>;

  return (
    <section className="max-w-xl">
      <h2 className="text-2xl font-medium">Payments</h2>
      <div className="mt-4 rounded-xl bg-[#0b3d6e] p-5">
        <p className="text-sm uppercase tracking-[0.14em] text-[#8fb3da]">Paystack</p>
        <p className="mt-2 text-2xl font-medium">{payments.mode === "live" ? "Live" : "Demo"}</p>
        <p className="mt-3 text-[#dde8f4]">
          {payments.mode === "live"
            ? "Cabinets charge a real card. The secret stays on the API."
            : "No card is charged. Set PAYSTACK_SECRET_KEY on the API when you are ready to take real payments."}
        </p>
        <p className="mt-4 text-sm text-[#b8cfe8]">Return URL host: {payments.public_base_url}</p>
      </div>
    </section>
  );
}

function InstallPage({ onError }: { onError: (message: string | null) => void }) {
  const [reports, setReports] = useState<InstallReport[]>([]);

  useEffect(() => {
    call<{ data: InstallReport[] }>("/admin/api/commission")
      .then((body) => setReports(body.data))
      .catch((reason) => onError(reason.message));
  }, [onError]);

  return (
    <section>
      <h2 className="text-2xl font-medium">Install</h2>
      <ol className="mt-3 max-w-2xl list-decimal space-y-1 pl-5 text-[#dde8f4]">
        <li>Do not make the app the home screen until one coil turns and one payment completes.</li>
        <li>On the cabinet, open operator settings and choose Install tests.</li>
        <li>Write down the machine number, the serial port that exists, the baud, and which coil moves for lane 1.</li>
        <li>Walk every row and coil. Mark what you see: turned, stuck, or empty.</li>
        <li>Take one payment on a coil that turned. Marks are sent here as you go. Save closes that visit.</li>
      </ol>
      {reports.length === 0 ? <p className="mt-6 text-[#b8cfe8]">No install has been recorded yet.</p> : null}
      <div className="mt-6 grid gap-6">
        {reports.map((report) => (
          <article key={report.id} className="rounded-xl bg-[#0b3d6e] p-5">
            <h3 className="text-xl font-medium">
              {report.machine_no}{" "}
              <span className="text-sm font-medium text-[#ffb366]">{report.status === "saved" ? "Saved" : "In progress"}</span>
            </h3>
            <p className="mt-1 text-sm text-[#b8cfe8]">
              {when(report.inserted_at)} · {report.board} · {report.serial_device || "no port"} · {report.baud} baud
              {report.simulate ? " · rehearsal" : " · live motor"}
            </p>
            <p className="mt-3">
              {report.rows} rows × {report.coils} coils · turned {report.turned} · stuck {report.stuck} · empty {report.empty} · not
              tested {report.untested}
            </p>
            <p className="mt-1">
              Payment {report.payment_status || "not run"}
              {report.payment_lane ? ` on lane ${report.payment_lane}` : ""}
              {report.payment_reference ? ` · ${report.payment_reference}` : ""}
            </p>
            <div className="mt-4 flex flex-wrap gap-2">
              {report.checks.map((check) => (
                <span
                  key={check.lane}
                  className={`rounded-lg px-2 py-1 text-sm ${coilTone(check.result)}`}
                  title={check.detail}
                >
                  {check.row}-{check.coil} {check.result}
                </span>
              ))}
            </div>
          </article>
        ))}
      </div>
    </section>
  );
}

function coilTone(result: string) {
  if (result === "turned") return "bg-[#ffb366] text-[#4d1d00]";
  if (result === "stuck") return "bg-[#4d1d00] text-[#ffb366]";
  if (result === "empty") return "bg-[#1f4a78] text-[#fff4e8]";
  return "bg-white/10 text-[#fff4e8]";
}

function ReleaseForm({ onError }: { onError: (message: string | null) => void }) {
  const [release, setRelease] = useState<Release | null>(null);

  useEffect(() => {
    call<{ data: Release }>("/admin/api/release")
      .then((body) => setRelease(body.data))
      .catch((reason) => onError(reason.message));
  }, [onError]);

  if (!release) return <p>Loading the current release…</p>;

  async function save(event: FormEvent) {
    event.preventDefault();
    if (!release) return;
    onError(null);
    try {
      const body = await call<{ data: Release }>("/admin/api/release", {
        method: "PUT",
        body: JSON.stringify(release),
      });
      setRelease(body.data);
    } catch (reason) {
      onError(reason instanceof Error ? reason.message : "Could not save the release");
    }
  }

  return (
    <section>
      <h2 className="text-2xl font-medium">Remote update</h2>
      <p className="mt-2 max-w-xl text-[#dde8f4]">
        Cabinets install this only when the version code is higher than the one they are running and the SHA-256 matches the file.
      </p>
      <form className="mt-4 grid max-w-xl gap-3" onSubmit={save}>
        <Field label="Version" value={release.version} onChange={(version) => setRelease({ ...release, version })} />
        <Field label="Version code" value={String(release.version_code)} onChange={(value) => setRelease({ ...release, version_code: Number(value) })} />
        <Field label="APK URL" value={release.url} onChange={(url) => setRelease({ ...release, url })} />
        <Field label="SHA-256" value={release.sha256} onChange={(sha256) => setRelease({ ...release, sha256 })} />
        <Field label="Required current version" value={release.required_version} onChange={(required_version) => setRelease({ ...release, required_version })} />
        <label className="grid gap-1 text-sm">
          Notes
          <textarea className="rounded-lg border border-[#6699cc]/40 bg-[#0b3d6e] px-3 py-2" value={release.notes} onChange={(event) => setRelease({ ...release, notes: event.target.value })} />
        </label>
        <button className="rounded-lg bg-[#ff6600] text-[#fff4e8] px-4 py-3 text-white" type="submit">
          Publish
        </button>
      </form>
    </section>
  );
}

function Field({ label, value, onChange }: { label: string; value: string; onChange: (value: string) => void }) {
  return (
    <label className="grid gap-1 text-sm">
      {label}
      <input className="rounded-lg border border-[#6699cc]/40 bg-[#0b3d6e] px-3 py-2" value={value} onChange={(event) => onChange(event.target.value)} />
    </label>
  );
}
