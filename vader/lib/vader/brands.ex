defmodule Vader.Brands do
  import Ecto.Query
  alias Vader.Machines.Brand
  alias Vader.Repo

  def list_brands do
    Repo.all(from b in Brand, order_by: [asc: b.name])
  end

  def get_by_slug(slug) when is_binary(slug) and slug != "" do
    Repo.get_by(Brand, slug: slug)
  end

  def get_by_slug(_), do: nil

  def import! do
    path = Application.app_dir(:vader, "priv/brands.json")

    path
    |> File.read!()
    |> Jason.decode!()
    |> Enum.each(fn row ->
      brand = Repo.get_by(Brand, slug: row["slug"]) || %Brand{}

      brand
      |> Brand.changeset(%{
        slug: row["slug"],
        name: row["name"],
        category: row["category"],
        image_url: row["image_url"]
      })
      |> Repo.insert_or_update!()
    end)

    :ok
  end

  def apply_to_good(attrs) when is_map(attrs) do
    attrs = Map.new(attrs, fn {key, val} -> {to_string(key), val} end)
    brand = get_by_slug(value(attrs, "brand_slug")) || match_name(value(attrs, "name"))

    case brand do
      nil ->
        attrs

      brand ->
        attrs
        |> put_value("brand_id", brand.id)
        |> put_value("image_url", brand.image_url)
        |> put_value("category", brand.category)
        |> put_name(brand)
    end
  end

  def match_name(name) when is_binary(name) do
    needle = normalize(name)

    if needle == "" do
      nil
    else
      list_brands()
      |> Enum.filter(fn brand ->
        label = normalize(brand.name)
        needle == label or String.starts_with?(needle, label <> " ")
      end)
      |> Enum.max_by(fn brand -> String.length(normalize(brand.name)) end, fn -> nil end)
    end
  end

  def match_name(_), do: nil

  def json(%Brand{} = brand) do
    %{
      id: brand.id,
      slug: brand.slug,
      name: brand.name,
      category: brand.category,
      image_url: brand.image_url
    }
  end

  defp put_name(attrs, brand) do
    case value(attrs, "name") do
      name when is_binary(name) and name != "" -> attrs
      _ -> put_value(attrs, "name", brand.name)
    end
  end

  defp put_value(attrs, key, val), do: Map.put(attrs, key, val)

  defp value(attrs, key), do: Map.get(attrs, key)

  defp normalize(name) do
    name
    |> String.downcase()
    |> String.replace(~r/['’]/u, "")
    |> String.replace(~r/[^a-z0-9]+/u, " ")
    |> String.trim()
  end
end
