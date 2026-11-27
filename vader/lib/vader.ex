defmodule Vader do
  @moduledoc """
  Vader keeps the contexts that define your domain
  and business logic.

  Contexts are also responsible for managing your data, regardless
  if it comes from the database, an external API or others.
  """

  def cash_enabled?, do: Vader.Settings.cash_enabled?()
end
