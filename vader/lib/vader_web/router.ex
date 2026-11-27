defmodule VaderWeb.Router do
  use VaderWeb, :router

  pipeline :api do
    plug :accepts, ["json"]
  end

  pipeline :admin do
    plug :accepts, ["json"]
    plug VaderWeb.Plugs.AdminAuth
  end

  scope "/api/app", VaderWeb do
    pipe_through :api

    get "/v1/goods/getGoodsList", KioskController, :goods
    post "/payments/paystack/initialize", KioskController, :initialize
    get "/payments/paystack/verify", KioskController, :verify
    post "/payments/paystack/refund", KioskController, :refund
    post "/payments/paystack/webhook", KioskController, :webhook
    post "/v1/order/updateOrderDeliver", KioskController, :deliver
    get "/v2/appLastedVersion", KioskController, :latest
    post "/v1/machine/appUpgradeResult", KioskController, :upgrade
    post "/v1/machine/commission", KioskController, :commission
    post "/v1/machine/climate", KioskController, :climate
  end

  scope "/api/app", VaderWeb do
    pipe_through :admin
    post "/payments/cash", KioskController, :cash
  end

  scope "/admin/api", VaderWeb.Admin do
    pipe_through :api
    post "/session", SessionController, :create
  end

  scope "/admin/api", VaderWeb.Admin do
    pipe_through :admin

    get "/overview", OverviewController, :show
    get "/payments", PaymentController, :show
    get "/machines", MachineController, :index
    post "/machines", MachineController, :create
    patch "/machines/:id", MachineController, :update
    get "/brands", BrandController, :index
    get "/goods", GoodController, :index
    post "/goods", GoodController, :create
    patch "/goods/:id", GoodController, :update
    delete "/goods/:id", GoodController, :delete
    get "/orders", OrderController, :index
    post "/orders/:id/refund", OrderController, :refund
    get "/commission", CommissionController, :index
    get "/release", ReleaseController, :show
    put "/release", ReleaseController, :update
    get "/settings", SettingsController, :show
    put "/settings", SettingsController, :update
  end

  scope "/", VaderWeb do
    get "/health", KioskController, :health
    get "/pay/demo", KioskController, :demo
    post "/pay/demo/confirm", KioskController, :confirm_demo
    get "/pay/return", KioskController, :pay_return
  end
end
